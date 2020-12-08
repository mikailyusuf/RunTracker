package com.mikail.runtracker.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.mikail.runtracker.R
import kotlinx.android.synthetic.main.fragment_run.*


class RunFragment : Fragment(R.layout.fragment_run) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab.setOnClickListener {

            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }
        
    }


}