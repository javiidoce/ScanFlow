package com.example.scanflow

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.io.File

class ProcesadasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FacturaProcesadaAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_procesadas, container, false)
        recyclerView = view.findViewById(R.id.recyclerProcesadas)
        recyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onResume() {
        super.onResume()
        mostrarFacturasProcesadas()
    }

    private fun mostrarFacturasProcesadas() {
        val dir = File(requireContext().filesDir, "procesadas")
        if (!dir.exists()) {
            Toast.makeText(context, "No hay facturas procesadas", Toast.LENGTH_SHORT).show()
            return
        }

        val archivos = dir.listFiles { file -> file.extension == "json" }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

        val facturas = archivos.mapNotNull { file ->
            try {
                val json = JSONObject(file.readText())
                val proveedor = json.optJSONObject("proveedor")?.optString("nombre") ?: "Desconocido"
                val fecha = json.optJSONObject("factura")?.optString("fecha_pedido") ?: "¿?"
                FacturaProcesada(file.nameWithoutExtension, proveedor, fecha, file)
            } catch (e: Exception) {
                Log.e("JSON_ERROR", "Error leyendo ${file.name}: ${e.message}")
                null
            }
        }

        adapter = FacturaProcesadaAdapter(facturas) { factura ->
            val fragment = DetalleFacturaProcesadaFragment().apply {
                arguments = Bundle().apply {
                    putString("archivo", factura.nombreArchivo)
                }
            }
            (activity as MainActivity).loadFragment(fragment)
        }

        recyclerView.adapter = adapter
    }
}


