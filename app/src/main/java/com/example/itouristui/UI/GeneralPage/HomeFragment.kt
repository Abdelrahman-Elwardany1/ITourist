package com.example.itouristui.UI.GeneralPage


import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itouristui.Adapters.horizontal_recyclerview_adapter
import com.example.itouristui.R
import kotlinx.android.synthetic.main.fragment_home.*
class HomeFragment : Fragment(){

    var wasPreviouslyCreated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            println(wasPreviouslyCreated)
            if (wasPreviouslyCreated.not()){
                wasPreviouslyCreated=true
            }
            CurrentLocationTextView.text = it.getString("CURRENT_LOCATION")
        }?:Toast.makeText(requireContext(),"A Problem has occurred,You may need to restart",Toast.LENGTH_LONG).show()

        with(SuggestedPlacesRecyclerView){
            layoutManager = LinearLayoutManager(requireContext() , LinearLayoutManager.HORIZONTAL , false)
            adapter = horizontal_recyclerview_adapter()
        }

        with(PopularPlacesRecyclerView){
            layoutManager = LinearLayoutManager(requireContext() , LinearLayoutManager.HORIZONTAL , false)
            adapter = horizontal_recyclerview_adapter()
        }

        with(NearbyPlacesRecyclerView){
            layoutManager = LinearLayoutManager(requireContext() , LinearLayoutManager.HORIZONTAL , false)
            adapter = horizontal_recyclerview_adapter()
        }

        val ss: SpannableString = SpannableString("See More")
        val underLineSpan = UnderlineSpan()

        ss.setSpan(underLineSpan, 0, 8, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        tVId5.text = ss
    }

}