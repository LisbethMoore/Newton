package com.lisbeth.newton

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HistorialActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var lista: MutableList<Simulacion>
    private lateinit var adapter: SimulacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        recycler = findViewById(R.id.recyclerHistorial)
        recycler.layoutManager = LinearLayoutManager(this)

        lista = mutableListOf()
        adapter = SimulacionAdapter(lista)
        recycler.adapter = adapter

        cargarDatos()
    }

    private fun cargarDatos() {

        val usuarioActual = FirebaseAuth.getInstance().currentUser?.email

        val ref = FirebaseDatabase.getInstance().getReference("simulaciones")

        ref.addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                lista.clear()

                for (dato in snapshot.children) {
                    val sim = dato.getValue(Simulacion::class.java)

                    // 🔥 FILTRO POR USUARIO
                    if (sim != null && sim.usuario == usuarioActual) {
                        lista.add(sim)
                    }
                }

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}