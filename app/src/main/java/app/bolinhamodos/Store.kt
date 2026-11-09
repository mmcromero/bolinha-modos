package app.bolinhamodos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Mode(
    val id: String,
    var name: String,
    var prompt: String,
    var offCommand: String
)

object Store {
    private const val PREFS = "bolinha_modos"
    private const val KEY_MODES = "modes"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(ctx: Context): MutableList<Mode> {
        val raw = prefs(ctx).getString(KEY_MODES, null)
        if (raw == null) {
            val seed = Defaults.modes()
            save(ctx, seed)
            return seed
        }
        return try {
            parse(raw)
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun save(ctx: Context, modes: List<Mode>) {
        prefs(ctx).edit().putString(KEY_MODES, toJson(modes)).apply()
    }

    fun toJson(modes: List<Mode>): String {
        val arr = JSONArray()
        for (m in modes) {
            val o = JSONObject()
            o.put("id", m.id)
            o.put("name", m.name)
            o.put("prompt", m.prompt)
            o.put("offCommand", m.offCommand)
            arr.put(o)
        }
        return arr.toString(2)
    }

    fun parse(raw: String): MutableList<Mode> {
        val out = mutableListOf<Mode>()
        val arr = JSONArray(raw)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.optString("name")
            if (name.isBlank()) continue
            val id = o.optString("id", "").ifBlank { UUID.randomUUID().toString() }
            val off = o.optString("offCommand", "").ifBlank { "Desativar " + name.lowercase() }
            out.add(Mode(id, name, o.optString("prompt"), off))
        }
        return out
    }

    fun isOn(ctx: Context, id: String): Boolean =
        prefs(ctx).getBoolean("on_" + id, false)

    fun setOn(ctx: Context, id: String, on: Boolean) {
        prefs(ctx).edit().putBoolean("on_" + id, on).apply()
    }

    fun anyOn(ctx: Context): Boolean =
        load(ctx).any { isOn(ctx, it.id) }
}
