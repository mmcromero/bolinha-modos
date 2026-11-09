package app.bolinhamodos

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.UUID

class MainActivity : Activity() {

    private val reqExport = 11
    private val reqImport = 12
    private val reqOverlay = 13

    private lateinit var status: TextView
    private lateinit var toggleBtn: Button
    private lateinit var listBox: LinearLayout

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(24), dp(16), dp(24))
        scroll.addView(root)

        val title = TextView(this)
        title.text = "Bolinha de modos"
        title.textSize = 26f
        title.gravity = Gravity.START
        root.addView(title)

        status = TextView(this)
        status.textSize = 16f
        status.setPadding(0, dp(8), 0, dp(8))
        root.addView(status)

        toggleBtn = Button(this)
        toggleBtn.textSize = 18f
        toggleBtn.setOnClickListener { toggleBubble() }
        root.addView(toggleBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(72)))

        val hint = TextView(this)
        hint.text = "Toque na bolinha, ligue um modo e cole o texto copiado na conversa."
        hint.textSize = 14f
        hint.setPadding(0, dp(8), 0, dp(16))
        root.addView(hint)

        val modesTitle = TextView(this)
        modesTitle.text = "Modos"
        modesTitle.textSize = 20f
        root.addView(modesTitle)

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        root.addView(listBox)

        val newBtn = Button(this)
        newBtn.text = "Novo modo"
        newBtn.setOnClickListener { editMode(null) }
        root.addView(newBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)))

        val exportBtn = Button(this)
        exportBtn.text = "Exportar modos"
        exportBtn.setOnClickListener { exportModes() }
        root.addView(exportBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)))

        val importBtn = Button(this)
        importBtn.text = "Importar modos"
        importBtn.setOnClickListener { importModes() }
        root.addView(importBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(64)))

        setContentView(scroll)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        refreshModes()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (OverlayService.running) {
            status.text = "Bolinha ligada"
            toggleBtn.text = "Fechar bolinha"
        } else {
            status.text = "Bolinha desligada"
            toggleBtn.text = "Mostrar bolinha"
        }
    }

    private fun toggleBubble() {
        if (OverlayService.running) {
            stopService(Intent(this, OverlayService::class.java))
        } else {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permita \"Exibir sobre outros apps\" e volte.", Toast.LENGTH_LONG).show()
                val i = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)
                )
                startActivityForResult(i, reqOverlay)
                return
            }
            startForegroundService(Intent(this, OverlayService::class.java))
        }
        Handler(Looper.getMainLooper()).postDelayed({ refreshStatus() }, 500)
    }

    private fun refreshModes() {
        listBox.removeAllViews()
        val modes = Store.load(this)
        for (m in modes) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setPadding(0, dp(8), 0, dp(8))

            val name = TextView(this)
            name.text = m.name
            name.textSize = 18f
            card.addView(name)

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL

            val edit = Button(this)
            edit.text = "Editar"
            edit.setOnClickListener { editMode(m) }
            row.addView(edit, LinearLayout.LayoutParams(0, dp(56), 1f))

            val del = Button(this)
            del.text = "Excluir"
            del.setOnClickListener { confirmDelete(m) }
            row.addView(del, LinearLayout.LayoutParams(0, dp(56), 1f))

            card.addView(row)
            listBox.addView(card)
        }
    }

    private fun confirmDelete(m: Mode) {
        AlertDialog.Builder(this)
            .setTitle("Excluir " + m.name + "?")
            .setPositiveButton("Excluir") { _, _ ->
                val list = Store.load(this)
                list.removeAll { it.id == m.id }
                Store.save(this, list)
                Store.setOn(this, m.id, false)
                refreshModes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun editMode(existing: Mode?) {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(8), dp(20), 0)

        val nameEt = EditText(this)
        nameEt.hint = "Nome do modo"
        nameEt.setText(existing?.name ?: "")
        box.addView(nameEt)

        val offEt = EditText(this)
        offEt.hint = "Comando para desativar"
        offEt.setText(existing?.offCommand ?: "")
        box.addView(offEt)

        val promptEt = EditText(this)
        promptEt.hint = "Prompt completo do modo"
        promptEt.setText(existing?.prompt ?: "")
        promptEt.minLines = 6
        promptEt.gravity = Gravity.TOP
        promptEt.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        box.addView(promptEt)

        val scroll = ScrollView(this)
        scroll.addView(box)

        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Novo modo" else "Editar modo")
            .setView(scroll)
            .setPositiveButton("Salvar") { _, _ ->
                val name = nameEt.text.toString().trim()
                if (name.isEmpty()) {
                    Toast.makeText(this, "Dê um nome ao modo.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                var off = offEt.text.toString().trim()
                if (off.isEmpty()) off = "Desativar " + name.lowercase()
                val prompt = promptEt.text.toString()

                val list = Store.load(this)
                if (existing == null) {
                    list.add(Mode(UUID.randomUUID().toString(), name, prompt, off))
                } else {
                    val target = list.firstOrNull { it.id == existing.id }
                    if (target != null) {
                        target.name = name
                        target.offCommand = off
                        target.prompt = prompt
                    }
                }
                Store.save(this, list)
                refreshModes()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun exportModes() {
        val i = Intent(Intent.ACTION_CREATE_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "application/json"
        i.putExtra(Intent.EXTRA_TITLE, "modos.json")
        startActivityForResult(i, reqExport)
    }

    private fun importModes() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(i, reqImport)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqOverlay) {
            refreshStatus()
            return
        }
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        if (requestCode == reqExport) {
            try {
                contentResolver.openOutputStream(uri)?.use {
                    it.write(Store.toJson(Store.load(this)).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "Modos exportados.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Não consegui exportar.", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == reqImport) {
            try {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: ""
                val incoming = Store.parse(raw)
                val list = Store.load(this)
                for (m in incoming) {
                    val same = list.firstOrNull { it.name == m.name }
                    if (same != null) {
                        same.prompt = m.prompt
                        same.offCommand = m.offCommand
                    } else {
                        list.add(m)
                    }
                }
                Store.save(this, list)
                refreshModes()
                Toast.makeText(this, "Modos importados: " + incoming.size, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Arquivo inválido.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
