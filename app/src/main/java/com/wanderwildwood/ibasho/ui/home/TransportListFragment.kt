package com.wanderwildwood.ibasho.ui.home

import android.os.Bundle
import com.wanderwildwood.ibasho.ui.paging.turnsAPageOnSwipe
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.transports.availableTransports
import com.wanderwildwood.ibasho.ui.TaggedFragment


class TransportListFragment : TaggedFragment() {

    override fun getStaticTag() = "TransportListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transport_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val transportListAdapter = TransportListAdapter(activity as AppCompatActivity)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_transports)
        recyclerView.turnsAPageOnSwipe()
        recyclerView.adapter = transportListAdapter

        transportListAdapter.submitList(availableTransports(view.context))
    }
}
