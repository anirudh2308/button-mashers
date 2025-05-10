package com.example.buttonmashers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class OwnedTitlesFragment : Fragment() {

    private lateinit var dbHelper: GameDatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.owned_titles_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view_owned_titles)
        recyclerView.layoutManager = LinearLayoutManager(context)

        dbHelper = GameDatabaseHelper(
            requireContext(),
            { fileName -> resources.getIdentifier(fileName, "drawable", requireContext().packageName) }
        )

        val gameTitles = dbHelper.getAllGames().filter { it.owned }
        val adapter = GameTitleAdapter(gameTitles, dbHelper)
        recyclerView.adapter = adapter
    }
}
