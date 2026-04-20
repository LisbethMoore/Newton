package com.lisbeth.newton

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.lisbeth.newton.databinding.ActivityMenuBinding
import com.google.firebase.auth.FirebaseAuth

class Menu : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()

        binding.btnPrimera.setOnClickListener {
            val irPrimeraLey = Intent(this, PrimeraLey::class.java)
            startActivity(irPrimeraLey)
        }

        binding.btnSegunda.setOnClickListener {
            val irKotlin = Intent(this, SegundaLey::class.java)
            startActivity(irKotlin)
        }



        binding.btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        // cerrar sesion
        binding.imgCerrarSesion.setOnClickListener {
            auth.signOut()

            val intent = Intent(this, MainActivity::class.java) // o LoginActivity
            startActivity(intent)
            finish()
        }
    }


    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            startActivity(Intent(this, MainActivity::class.java)) // o LoginActivity
            finish()
        }
    }
}