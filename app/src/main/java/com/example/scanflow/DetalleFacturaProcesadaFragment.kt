package com.example.scanflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.io.File

class DetalleFacturaProcesadaFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.item_detalle_factura_procesada, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val archivo = arguments?.getString("archivo") ?: return
        val file = File(requireContext().filesDir, "procesadas/$archivo.json")

        if (!file.exists()) return

        val json = JSONObject(file.readText())
        val proveedor = json.getJSONObject("proveedor")
        val factura = json.getJSONObject("factura")
        val lineas = json.getJSONArray("lineas")

        view.findViewById<TextView>(R.id.textProveedor).text = proveedor.getString("nombre")
        view.findViewById<TextView>(R.id.textFecha).text = factura.getString("fecha_pedido")
        view.findViewById<TextView>(R.id.textImporte).text = factura.getString("total_factura") + " €"

        val layoutLineas = view.findViewById<LinearLayout>(R.id.layoutLineas)
        for (i in 0 until lineas.length()) {
            val linea = lineas.getJSONObject(i)
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_linea_factura, layoutLineas, false)
            itemView.findViewById<TextView>(R.id.textDescripcion).text = linea.getString("descripcion")
            itemView.findViewById<TextView>(R.id.textTotal).text = linea.getString("total_linea") + " €"
            layoutLineas.addView(itemView)
        }
    }
}