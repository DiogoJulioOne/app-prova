package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.R
import com.example.gametracker.dados.JogoRepository
import com.example.gametracker.dados.UsuarioRepository
import com.example.gametracker.modelos.Jogo
import kotlinx.coroutines.launch
import java.util.UUID

class AdicionarJogoActivity : AppCompatActivity() {
    private val jogos by lazy { JogoRepository(this) }
    private val usuarios by lazy { UsuarioRepository(this) }
    private var idJogo: String? = null
    private var usuarioId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adicionar_jogo)

        val titulo = findViewById<EditText>(R.id.editTituloJogo)
        val plataforma = findViewById<EditText>(R.id.editPlataforma)
        val dataInicio = findViewById<EditText>(R.id.editDataInicio)
        val horasJogadas = findViewById<EditText>(R.id.editHorasJogadas)
        val progresso = findViewById<EditText>(R.id.editProgresso)
        val observacoes = findViewById<EditText>(R.id.editObservacoes)
        val status = findViewById<Spinner>(R.id.spinnerStatus)
        val statusOptions = resources.getStringArray(R.array.status_jogo)
        status.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statusOptions
        )

        idJogo = intent.getStringExtra(EXTRA_ID_JOGO)
        val botaoSalvar = findViewById<Button>(R.id.buttonSalvarJogo)
        botaoSalvar.isEnabled = false
        if (idJogo != null) {
            title = getString(R.string.editar_jogo)
            botaoSalvar.setText(R.string.salvar)
        }

        lifecycleScope.launch {
            val usuario = usuarios.usuarioDaSessao()
            if (usuario == null) {
                abrirLogin()
                return@launch
            }
            usuarioId = usuario.id
            if (idJogo != null) {
                val jogo = jogos.buscarPorId(idJogo!!, usuario.id)
                if (jogo == null) {
                    finish()
                    return@launch
                }
                titulo.setText(jogo.titulo)
                plataforma.setText(jogo.plataforma)
                dataInicio.setText(jogo.dataInicio)
                jogo.horasJogadas?.let { horasJogadas.setText(formatHoras(it)) }
                jogo.progresso?.let { progresso.setText(it.toString()) }
                observacoes.setText(jogo.observacoes)
                val statusSalvo = if (jogo.status.equals("Concluído", ignoreCase = true)) {
                    getString(R.string.status_zerado)
                } else {
                    jogo.status
                }
                val indiceStatus = statusOptions.indexOf(statusSalvo)
                if (indiceStatus >= 0) status.setSelection(indiceStatus)
            }
            botaoSalvar.isEnabled = true
        }

        botaoSalvar.setOnClickListener { botao ->
            val nome = titulo.text.toString().trim()
            val plataformaDigitada = plataforma.text.toString().trim()
            if (nome.isBlank() || plataformaDigitada.isBlank()) {
                Toast.makeText(this, R.string.erro_campos_jogo, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val progressoDigitado = progresso.text.toString().trim()
            val progressoValor = if (progressoDigitado.isBlank()) {
                null
            } else {
                progressoDigitado.toIntOrNull()?.takeIf { it in 0..100 }
            }
            if (progressoDigitado.isNotBlank() && progressoValor == null) {
                progresso.error = getString(R.string.erro_progresso)
                return@setOnClickListener
            }
            val horasDigitadas = horasJogadas.text.toString().trim().replace(',', '.')
            val horasValor = if (horasDigitadas.isBlank()) {
                null
            } else {
                horasDigitadas.toDoubleOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
            }
            if (horasDigitadas.isNotBlank() && horasValor == null) {
                horasJogadas.error = getString(R.string.erro_horas)
                return@setOnClickListener
            }
            val donoId = usuarioId ?: return@setOnClickListener
            botao.isEnabled = false
            lifecycleScope.launch {
                jogos.salvar(
                    Jogo(
                        id = idJogo ?: UUID.randomUUID().toString(),
                        titulo = nome,
                        plataforma = plataformaDigitada,
                        status = status.selectedItem.toString(),
                        dataInicio = dataInicio.text.toString().trim(),
                        horasJogadas = horasValor,
                        progresso = progressoValor?.coerceIn(0, 100),
                        observacoes = observacoes.text.toString().trim(),
                        usuarioId = donoId
                    ),
                    donoId
                )
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID_JOGO = "id_jogo"
    }

    private fun formatHoras(horas: Double): String =
        if (horas % 1.0 == 0.0) horas.toInt().toString() else horas.toString()

    private fun abrirLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
