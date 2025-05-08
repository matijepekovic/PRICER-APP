// Add this import at the top of MainActivity.kt or your Util file
import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Inside MainActivity class (or a FileUtils object)
/**
 * Copies content from a source URI (obtained via ContentResolver) to a destination File.
 * Returns true on success, false on failure.
 */
private fun copyUriToFile(context: Context, sourceUri: Uri, destinationFile: File): Boolean {
    Log.d("FileUtils", "Attempting to copy URI $sourceUri to File ${destinationFile.absolutePath}")
    var inputStream: InputStream? = null
    var outputStream: FileOutputStream? = null
    try {
        inputStream = context.contentResolver.openInputStream(sourceUri)
        outputStream = FileOutputStream(destinationFile)
        if (inputStream != null) {
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush() // Ensure data is written
            Log.i("FileUtils", "Successfully copied content to ${destinationFile.name}")
            return true
        } else {
            Log.e("FileUtils", "Failed to open InputStream for source URI.")
            return false
        }
    } catch (e: Exception) {
        Log.e("FileUtils", "Error copying file from URI", e)
        // Attempt to delete partially written destination file on error
        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        return false
    } finally {
        // Ensure streams are closed
        try { inputStream?.close() } catch (e: IOException) { Log.e("FileUtils", "Error closing input stream", e)}
        try { outputStream?.close() } catch (e: IOException) { Log.e("FileUtils", "Error closing output stream", e)}
    }
}