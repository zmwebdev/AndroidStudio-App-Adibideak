import android.content.Context
import java.io.InputStream

object JsonUtils {
    fun loadJSONFromRaw(context: Context, resourceId: Int): String {
        val inputStream: InputStream = context.resources.openRawResource(resourceId)
        return inputStream.bufferedReader().use { it.readText() }
    }
}