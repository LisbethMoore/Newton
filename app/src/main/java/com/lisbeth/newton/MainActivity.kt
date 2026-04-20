package com.lisbeth.newton

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.lisbeth.newton.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val correo = binding.etCorreo.text.toString().trim()
            val contrasena = binding.etContrasena.text.toString().trim()

            if (correo.isEmpty()) {
                binding.etCorreo.error = "Ingresa tu correo"
                binding.etCorreo.requestFocus()
                return@setOnClickListener
            }

            if (contrasena.isEmpty()) {
                binding.etContrasena.error = "Ingresa tu contraseña"
                binding.etContrasena.requestFocus()
                return@setOnClickListener
            }

            iniciarSesion(correo, contrasena)
        }

        binding.btnIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            startActivity(Intent(this, Menu::class.java))
            finish()
        }
    }

    private fun iniciarSesion(correo: String, contrasena: String) {
        auth.signInWithEmailAndPassword(correo, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Menu::class.java))
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Error al iniciar sesión: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
}