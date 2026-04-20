package com.lisbeth.newton

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.lisbeth.newton.databinding.ActivitySegundaLeyBinding

// 🔥 IMPORTS FIREBASE
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SegundaLey : AppCompatActivity() {

    data class ResultadoAnalisis(
        val estado: String,
        val comportamiento: String,
        val peso: Double,
        val normal: Double,
        val friccionMaxima: Double,
        val fuerzaNeta: Double,
        val aceleracion: Double,
        val velocidadFinal: Double,
        val desplazamiento: Double,
        val tipoEstado: SimulacionSegundaLeyView.EstadoSimulacion,
        val explicacion: String
    )

    private lateinit var binding: ActivitySegundaLeyBinding
    private var explicacionActual: String? = null
    private var muActual: Double = 0.0
    private var nombreSuperficieActual: String = "Hielo"
    private lateinit var nombresSuperificies: Array<String>
    private lateinit var valoresMu: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySegundaLeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nombresSuperificies = resources.getStringArray(R.array.lista_superficies)
        valoresMu           = resources.getStringArray(R.array.lista_mu)

        configurarSpinner()
        configurarEstadoInicial()

        binding.btnAnalizar.setOnClickListener { calcularMovimiento() }
        binding.btnExplicacion.setOnClickListener { mostrarOcultarExplicacion() }
    }

    private fun configurarSpinner() {
        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresSuperificies
        )
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerSuperficie.adapter = adaptador

        binding.spinnerSuperficie.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                nombreSuperficieActual = nombresSuperificies[position]
                val esPersonalizado = position == nombresSuperificies.size - 1
                if (esPersonalizado) {
                    binding.layoutMuPersonalizado.visibility = View.VISIBLE
                    binding.tvMuActual.text = "μ = (personalizado)"
                    muActual = 0.0
                } else {
                    binding.layoutMuPersonalizado.visibility = View.GONE
                    muActual = valoresMu[position].toDoubleOrNull() ?: 0.0
                    binding.tvMuActual.text = "μ = ${"%.2f".format(muActual)}"
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarEstadoInicial() {
        binding.vistaSimulacion.reiniciar()
        binding.btnExplicacion.visibility  = View.GONE
        binding.cardExplicacion.visibility = View.GONE
    }

    private fun calcularMovimiento() {

        val masa = leerDouble(binding.etMasa.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la masa"); return
        }
        val fuerzaAplicada = leerDouble(binding.etFuerzaAplicada.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la fuerza aplicada"); return
        }
        val velocidadInicial = leerDouble(binding.etVelocidadInicial.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para la velocidad inicial"); return
        }
        val tiempo = leerDouble(binding.etTiempo.text?.toString()) ?: run {
            mostrarError("Ingresa un valor válido para el tiempo"); return
        }

        val posicionSpinner = binding.spinnerSuperficie.selectedItemPosition
        val esPersonalizado = posicionSpinner == nombresSuperificies.size - 1
        if (esPersonalizado) {
            val muTexto = leerDouble(binding.etMuPersonalizado.text?.toString())
            if (muTexto == null) {
                mostrarError("Ingresa el coeficiente de fricción personalizado"); return
            }
            muActual = muTexto
        }

        if (masa <= 0.0) { mostrarError("La masa debe ser mayor que cero"); return }
        if (tiempo < 0.0) { mostrarError("El tiempo no puede ser negativo"); return }
        if (velocidadInicial < 0.0) { mostrarError("La velocidad inicial no puede ser negativa"); return }
        if (muActual < 0.0) { mostrarError("El coeficiente de fricción no puede ser negativo"); return }

        val g = 9.8
        val peso = masa * g
        val normal = peso
        val friccionMaxima = muActual * normal
        val fuerzaNeta = fuerzaAplicada - friccionMaxima
        val aceleracion = fuerzaNeta / masa
        val velocidadFinal = velocidadInicial + aceleracion * tiempo
        val desplazamiento = velocidadInicial * tiempo + 0.5 * aceleracion * tiempo * tiempo

        val resultado = ResultadoAnalisis(
            estado = if (fuerzaNeta > 0) "Aceleración" else "Sin movimiento",
            comportamiento = "Movimiento calculado",
            peso = peso,
            normal = normal,
            friccionMaxima = friccionMaxima,
            fuerzaNeta = fuerzaNeta,
            aceleracion = aceleracion,
            velocidadFinal = velocidadFinal,
            desplazamiento = desplazamiento,
            tipoEstado = SimulacionSegundaLeyView.EstadoSimulacion.ACELERACION_POSITIVA,
            explicacion = "Aplicando F = m·a"
        )

        // 🔥 GUARDAR EN FIREBASE
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("simulaciones")

        val usuario = FirebaseAuth.getInstance().currentUser?.email ?: "anonimo"

        val simulacion = Simulacion(
            usuario,
            masa,
            fuerzaAplicada,
            "a = ${"%.2f".format(aceleracion)}",
            "Segunda Ley"
        )

        ref.push().setValue(simulacion)

        // UI
        mostrarResultados(resultado)

        binding.vistaSimulacion.configurarSimulacion(
            estado = resultado.tipoEstado,
            texto = resultado.estado,
            fuerzaAplicada = fuerzaAplicada.toFloat(),
            friccionMaxima = friccionMaxima.toFloat(),
            fuerzaNeta = resultado.fuerzaNeta.toFloat(),
            aceleracion = resultado.aceleracion.toFloat(),
            masa = masa.toFloat(),
            velocidadInicialMs = velocidadInicial.toFloat(),
            velocidadFinalMs = resultado.velocidadFinal.toFloat(),
            tiempo = tiempo.toFloat(),
            superficie = nombreSuperficieActual
        )

        explicacionActual = resultado.explicacion
        binding.btnExplicacion.visibility = View.VISIBLE
        binding.cardExplicacion.visibility = View.GONE
        binding.btnExplicacion.text = "Ver explicación"
    }

    private fun mostrarResultados(r: ResultadoAnalisis) {
        binding.tvEstado.text = "Estado: ${r.estado}"
        binding.tvComportamiento.text = "Comportamiento: ${r.comportamiento}"
        binding.tvPeso.text = "Peso: ${"%.2f".format(r.peso)}"
        binding.tvAceleracion.text = "Aceleración: ${"%.2f".format(r.aceleracion)}"
    }

    private fun mostrarOcultarExplicacion() {
        if (explicacionActual == null) return
        if (binding.cardExplicacion.visibility == View.GONE) {
            binding.tvExplicacion.text = explicacionActual
            binding.cardExplicacion.visibility = View.VISIBLE
            binding.btnExplicacion.text = "Ocultar explicación"
        } else {
            binding.cardExplicacion.visibility = View.GONE
            binding.btnExplicacion.text = "Ver explicación"
        }
    }

    private fun leerDouble(texto: String?): Double? {
        if (texto.isNullOrBlank()) return null
        return texto.trim().toDoubleOrNull()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}