package com.lisbeth.newton

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lisbeth.newton.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnRegistrar.setOnClickListener {
            registrarUsuario()
        }
    }

    private fun registrarUsuario() {
        val nombre = binding.etNombre.text.toString().trim()
        val correo = binding.etCorreoRegistro.text.toString().trim()
        val contrasena = binding.etContrasenaRegistro.text.toString().trim()
        val confirmarContrasena = binding.etConfirmarContrasena.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.etNombre.error = "Ingresa tu nombre"
            binding.etNombre.requestFocus()
            return
        }

        if (correo.isEmpty()) {
            binding.etCorreoRegistro.error = "Ingresa tu correo"
            binding.etCorreoRegistro.requestFocus()
            return
        }

        if (contrasena.isEmpty()) {
            binding.etContrasenaRegistro.error = "Ingresa tu contraseña"
            binding.etContrasenaRegistro.requestFocus()
            return
        }

        if (contrasena.length < 6) {
            binding.etContrasenaRegistro.error = "La contraseña debe tener mínimo 6 caracteres"
            binding.etContrasenaRegistro.requestFocus()
            return
        }

        if (confirmarContrasena.isEmpty()) {
            binding.etConfirmarContrasena.error = "Confirma tu contraseña"
            binding.etConfirmarContrasena.requestFocus()
            return
        }

        if (contrasena != confirmarContrasena) {
            binding.etConfirmarContrasena.error = "Las contraseñas no coinciden"
            binding.etConfirmarContrasena.requestFocus()
            return
        }

        auth.createUserWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    android.util.Log.d("REGISTRO_FIREBASE", "OK uid=${user?.uid} email=${user?.email}")

                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, Menu::class.java))
                    finish()
                } else {
                    val e = task.exception
                    android.util.Log.e("REGISTRO_FIREBASE", "FALLO", e)

                    Toast.makeText(
                        this,
                        "Error: ${e?.javaClass?.simpleName}\n${e?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}