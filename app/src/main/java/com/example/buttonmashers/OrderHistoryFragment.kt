package com.example.buttonmashers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OrderHistoryFragment : Fragment() {

    private lateinit var orderRecyclerView: RecyclerView
    private lateinit var orderAdapter: OrderRowAdapter
    private lateinit var dbHelper: GameDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.order_history_fragment, container, false)

        orderRecyclerView = view.findViewById(R.id.recyclerViewOrders)
        dbHelper = GameDatabaseHelper(
            requireContext(),
            { fileName -> resources.getIdentifier(fileName, "drawable", context?.packageName) }
        )

        val orders = dbHelper.getAllOrders() // Fetch orders from the database
        orderAdapter = OrderRowAdapter(orders, requireContext())
        orderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        orderRecyclerView.adapter = orderAdapter
        val dividerItemDecoration = DividerItemDecoration(
            orderRecyclerView.context,
            (orderRecyclerView.layoutManager as LinearLayoutManager).orientation
        )
        orderRecyclerView.addItemDecoration(dividerItemDecoration)

        return view
    }
}
