package app.leafolio

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.leafolio.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TransferState(
    val busy: Boolean = false,
    val progress: TransferProgress = TransferProgress(""),
    val result: String? = null,
    val preview: RestorePreview? = null,
)

class TransferViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application
    private val db get() = LeafolioDatabase.get(context)
    private val mutableState = MutableStateFlow(TransferState())
    val state = mutableState.asStateFlow()
    private var restoreUri: Uri? = null

    fun dismissResult() { mutableState.value = mutableState.value.copy(result = null) }
    fun dismissPreview() { restoreUri = null; mutableState.value = mutableState.value.copy(preview = null) }

    private fun run(label: String, action: suspend () -> String) {
        if (mutableState.value.busy) return
        mutableState.value = TransferState(busy = true, progress = TransferProgress(label))
        viewModelScope.launch {
            try {
                val result = action()
                mutableState.value = mutableState.value.copy(busy = false, result = result)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = TransferState(result = "$label 失败：${error.message ?: "请检查文件和存储空间"}。未记录为成功。")
            }
        }
    }

    private fun progress(value: TransferProgress) { mutableState.value = mutableState.value.copy(progress = value) }

    fun backup(uri: Uri) = run("创建备份") {
        DataTransfer.createBackup(context, uri, db, onProgress = ::progress)
        BackupReminder.cancelNotification(context)
        val savedStatus = runCatching { UserPreferencesStore(context).markBackupSuccess() }.isSuccess
        if (savedStatus) "完整备份已保存。请妥善保管 ZIP 文件。"
        else "完整备份已保存，但最近成功时间未能更新。"
    }

    fun export(uri: Uri, selection: ExportScope) = run("通用导出") {
        DataTransfer.createPortableExport(context, uri, db, selection, ::progress)
        "通用文件已导出，可解压阅读 Markdown 日记与照片。"
    }

    fun inspect(uri: Uri) = run("检查备份") {
        val preview = DataTransfer.inspectBackup(context, uri)
        restoreUri = uri
        mutableState.value = mutableState.value.copy(preview = preview)
        ""
    }

    fun restore() {
        val uri = restoreUri ?: return
        restoreUri = null
        run("恢复备份") {
            val result = DataTransfer.restore(context, uri, db)
            "恢复完成：新增 ${result.added} 篇，跳过 ${result.skipped} 篇，冲突副本 ${result.conflicts} 篇，照片 ${result.photos} 张。" +
                if (result.settingsWarning) "日记和照片已恢复，但设置恢复失败，请在设置页手动调整。" else ""
        }
    }
}
