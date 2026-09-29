package com.example.gametracker.telas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.R
import com.example.gametracker.dados.JogoRepository
import com.example.gametracker.dados.UsuarioRepository
import com.example.gametracker.modelos.Usuario
import kotlinx.coroutines.launch

class EditarPerfilActivity : AppCompatActivity() {
    private val usuarios by lazy { UsuarioRepository(this) }
    private val jogos by lazy { JogoRepository(this) }

    private var usuarioAtual: Usuario? = null
    // avatarSelecionado contains either a built-in key (avatar_1..6) or a URI marker like "uri:<encodedUri>"
    private var avatarSelecionado: String = "avatar_1"
    private var selectedPhotoUri: Uri? = null
    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        findViewById<ImageButton>(R.id.buttonCancelarEditar).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonSalvarEditar).setOnClickListener { salvar() }
        findViewById<Button>(R.id.buttonCancelarFormulario).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonEscolherFoto).setOnClickListener { abrirPhotoPicker() }

// avatar selection
        val avatarIds = listOf(
            R.id.avatar1,
            R.id.avatar2,
            R.id.avatar3,
            R.id.avatar4,
            R.id.avatar5,
            R.id.avatar6,
            R.id.avatar7,
            R.id.avatar8,
            R.id.avatar9
        )
        avatarIds.forEachIndexed { index, id ->
            findViewById<ImageView>(id).setOnClickListener {
                // clear any selected photo uri when picking a built-in avatar
                selectedPhotoUri = null
                selecionarAvatar("avatar_${index + 1}")
                // small scale animation for selection feedback
                it.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).withEndAction { it.animate().scaleX(1f).scaleY(1f).duration = 120 }
            }
        }

        // register modern photo picker (PickVisualMedia). Fallback handled below if unavailable.
        // This contract returns a Uri or null.
        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                selectedPhotoUri = uri
                avatarSelecionado = "uri:$uri"

                val preview = findViewById<ImageView>(R.id.imagemAvatarPreview)
                preview.setImageURI(uri)

                atualizarSelecaoAvatar()
            }
        }

        lifecycleScope.launch {
            val u = usuarios.usuarioDaSessao()

            if (u == null) {
                finish()
                return@launch
            }

            usuarioAtual = u
            carregarDados(u)

            val biblioteca = jogos.listar(u.id)
            val titulos = biblioteca.map { it.titulo }

            val auto = findViewById<AutoCompleteTextView>(R.id.inputJogoFavorito)

            auto.setAdapter(
                ArrayAdapter(
                    this@EditarPerfilActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    titulos
                )
            )
        }

        // alternative launcher if PickVisualMedia not available could be added here (OpenDocument)
    }

    private fun abrirPhotoPicker() {
        // prefer images only
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        photoPickerLauncher.launch(request)
    }

    private fun carregarDados(u: Usuario) {
        findViewById<EditText>(R.id.inputNome).setText(u.nome)
        findViewById<EditText>(R.id.inputNickname).setText(u.nickname)
        findViewById<EditText>(R.id.inputEmail).setText(u.email)
        findViewById<EditText>(R.id.inputBio).setText(u.bio)
        findViewById<AutoCompleteTextView>(R.id.inputJogoFavorito).setText(u.jogoFavorito)
        avatarSelecionado = u.avatar.ifBlank { "avatar_1" }
        // set selectedPhotoUri if avatar is a uri/file; then update preview centrally
        if (avatarSelecionado.startsWith("uri:")) {
            val uriString = avatarSelecionado.removePrefix("uri:")
            try {
                val uri = Uri.parse(uriString)
                selectedPhotoUri = uri
            } catch (e: Exception) {
                avatarSelecionado = "avatar_1"
                selectedPhotoUri = null
            }
        } else if (avatarSelecionado.startsWith("file:")) {
            val path = avatarSelecionado.removePrefix("file:")
            try {
                val file = java.io.File(path)
                if (file.exists()) {
                    selectedPhotoUri = Uri.fromFile(file)
                } else {
                    avatarSelecionado = "avatar_1"
                    selectedPhotoUri = null
                }
            } catch (e: Exception) {
                avatarSelecionado = "avatar_1"
                selectedPhotoUri = null
            }
        } else {
            selectedPhotoUri = null
        }
        atualizarSelecaoAvatar()
        atualizarPreview()
    }

    private fun selecionarAvatar(chave: String) {
        avatarSelecionado = chave
        // selecting a built-in avatar clears any selected photo URI
        selectedPhotoUri = null
        atualizarSelecaoAvatar()
        atualizarPreview()
    }

    private fun atualizarPreview() {
        val preview = findViewById<ImageView>(R.id.imagemAvatarPreview)
        try {
            when {
                avatarSelecionado.startsWith("uri:") || selectedPhotoUri != null -> {
                    val uri = selectedPhotoUri ?: Uri.parse(avatarSelecionado.removePrefix("uri:"))
                    try {
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                            if (bmp != null) preview.setImageBitmap(bmp) else preview.setImageResource(R.drawable.avatar_1)
                        } ?: preview.setImageResource(R.drawable.avatar_1)
                    } catch (e: Exception) {
                        try { preview.setImageURI(uri) } catch (_: Exception) { preview.setImageResource(R.drawable.avatar_1) }
                    }
                }
                avatarSelecionado.startsWith("file:") -> {
                    val path = avatarSelecionado.removePrefix("file:")
                    val file = java.io.File(path)
                    if (file.exists()) {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) preview.setImageBitmap(bmp) else preview.setImageResource(R.drawable.avatar_1)
                    } else {
                        preview.setImageResource(R.drawable.avatar_1)
                    }
                }
                else -> {
                    val res = when (avatarSelecionado) {
                        "avatar_1" -> R.drawable.avatar_real_1
                        "avatar_2" -> R.drawable.avatar_real_2
                        "avatar_3" -> R.drawable.avatar_real_3
                        "avatar_4" -> R.drawable.avatar_real_4
                        "avatar_5" -> R.drawable.avatar_real_5
                        "avatar_6" -> R.drawable.avatar_real_6
                        "avatar_7" -> R.drawable.avatar_real_7
                        "avatar_8" -> R.drawable.avatar_real_8
                        "avatar_9" -> R.drawable.avatar_real_9
                        else -> R.drawable.avatar_real_1
                    }

                    preview.setImageResource(res)
                }
            }
        } catch (e: Exception) {
            preview.setImageResource(R.drawable.avatar_1)
        }
    }

    private fun atualizarSelecaoAvatar() {
        val mapping = mapOf(
            "avatar_1" to R.id.avatar1,
            "avatar_2" to R.id.avatar2,
            "avatar_3" to R.id.avatar3,
            "avatar_4" to R.id.avatar4,
            "avatar_5" to R.id.avatar5,
            "avatar_6" to R.id.avatar6,
            "avatar_7" to R.id.avatar7,
            "avatar_8" to R.id.avatar8,
            "avatar_9" to R.id.avatar9
        )

        mapping.values.forEach { findViewById<ImageView>(it).alpha = 0.6f }

        // if a built-in avatar is selected, highlight it
        mapping[avatarSelecionado]?.let { findViewById<ImageView>(it).alpha = 1.0f }
    }
    private fun salvar() {
        val nome = findViewById<EditText>(R.id.inputNome).text.toString().trim()
        val nickname = findViewById<EditText>(R.id.inputNickname).text.toString().trim()
        val email = findViewById<EditText>(R.id.inputEmail).text.toString().trim()
        val bio = findViewById<EditText>(R.id.inputBio).text.toString().trim()
        val jogoFavorito = findViewById<AutoCompleteTextView>(R.id.inputJogoFavorito).text.toString().trim()

        if (nickname.isBlank()) {
            Toast.makeText(this, "Nickname não pode ficar vazio", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "E-mail inválido", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // verifica duplicidade de email (outro usuário)
            if (email.isNotBlank()) {
                val encontrado = usuarios.buscarPorEmail(email)
                if (encontrado != null && encontrado.id != usuarioAtual?.id) {
                    Toast.makeText(this@EditarPerfilActivity, "E-mail já cadastrado para outro usuário", Toast.LENGTH_LONG).show()
                    return@launch
                }
            }

            val atual = usuarioAtual ?: return@launch
            // if user picked a photo from gallery, copy it to app-private storage so it remains available
            if (selectedPhotoUri != null) {
                try {
                    val uri = selectedPhotoUri!!
                    val input = contentResolver.openInputStream(uri)
                    input?.use { ins ->
                        val filename = "avatar_${atual.id}.jpg"
                        val outFile = java.io.File(filesDir, filename)
                        outFile.outputStream().use { fos ->
                            ins.copyTo(fos)
                        }
                        // save as file:<absolutePath> so loading code can detect and display it
                        avatarSelecionado = "file:${outFile.absolutePath}"
                    }
                } catch (e: Exception) {
                    // ignore copy failure; fallback to keeping avatarSelecionado as-is (may be uri:...)
                }
            }
            val atualizado = atual.copy(
                nome = nome.ifBlank { atual.nome },
                nickname = nickname,
                email = email,
                bio = bio,
                jogoFavorito = jogoFavorito,
                avatar = avatarSelecionado
            )
            usuarios.atualizarPerfil(atualizado)

            Toast.makeText(this@EditarPerfilActivity, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
            // smooth return transition
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
