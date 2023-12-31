package com.example.itouristui.UI.GeneralPage

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.itouristui.Adapters.CityPicturesRecyclerViewAdapter
import com.example.itouristui.Data.Remote.UnsplashData
import com.example.itouristui.Data.Remote.WikipediaData
import com.example.itouristui.FirebaseObj
import com.example.itouristui.R
import com.example.itouristui.UI.Tours.ToursActivity
import com.example.itouristui.Utilities.CustomRetrofitCallBack
import com.example.itouristui.iToursit
import com.example.itouristui.models.SimpleCityDetail
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_general.*
import kotlinx.android.synthetic.main.fragment_city_overview.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CityOverviewFragment : Fragment() {

    private var isLiked: Boolean = false
    var cityRef : DocumentReference? = null
    var totalLikes = 0
    data class LikedCity(val name: String)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_city_overview,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        CityDetailsShimmer.startShimmerAnimation()
        OtherImagesShimmer.startShimmerAnimation()

        favoriteImageViewID.setOnClickListener {
            onFavoriteClicked()
        }

        var cityNameExtra = ""
        arguments?.let {bundle->
           val cityName = bundle.getString("CITY","")
           val cityImageExtra = StringBuilder()
           CityNameTextView.text = cityName

           WikipediaData.wikiApiImp.getClosestResult(cityName.substringBefore(',')).enqueue(
               CustomRetrofitCallBack<String>{
                   val closestCityName = JSONArray(it.body()).getJSONArray(1).getString(0)
                    cityNameExtra = closestCityName
                   cityRef = FirebaseObj.fireStore.collection("City").document(closestCityName)

                   WikipediaData.wikiApiImp.getCityDataByName(closestCityName).enqueue(
                       CustomRetrofitCallBack<String>{
                           CityDetailsShimmer.apply {
                               stopShimmerAnimation()
                               visibility = View.GONE
                           }
                           CityDetailsTextView.visibility = View.VISIBLE

                           println("API : inside CityDetails Callback , Response = ${it.body()}")
                           try {
                                val cityPage = JSONObject(it.body()?:"").getJSONObject("query").getJSONArray("pages")
                                val cityDescription = cityPage.getJSONObject(0).getString("extract").takeIf { extract-> extract.isNotBlank() }?: throw JSONException("")
                                CityDetailsTextView.text = cityDescription
                            }catch (e:JSONException){
                               println("API : inside CityDetails Callback , Exception")
                               CityDetailsTextView.text = "Can not find Details of this city !"
                            }
                       }
                   )

                   WikipediaData.wikiApiImp.getCityImage(closestCityName).enqueue(
                       CustomRetrofitCallBack<String>{
                           CityNameGradiantLayout.visibility = View.VISIBLE

                           println("API : inside CityImage Callback , Response = ${it.body()}")
                           try {
                               val cityImagePage = JSONObject(it.body()?:"").getJSONObject("query").getJSONArray("pages")
                               val cityImage = cityImagePage.getJSONObject(0).getJSONObject("thumbnail").getString("source")
                               Glide.with(requireContext()).load(cityImage).into(CityImageView)
                               cityImageExtra.append(cityImage)
                               fetchFireStoreData(cityImage)
                           }catch (e:JSONException){
                               println("API : inside CityImage Callback , Exception")
                                CityImageView.setImageResource(R.drawable.notfound_404_error)
                           }
                       }
                   )

               }
           )



           UnsplashData.unsplashInterface.getCityImages(cityName.replace(',',' ')).enqueue(
               CustomRetrofitCallBack<String>{

                   OtherImagesShimmer.apply {
                       stopShimmerAnimation()
                       visibility = View.GONE
                   }
                   OtherImagesRecyclerView.visibility = View.VISIBLE

                   val mutableListOfImages = mutableListOf<String>()
                   val imagesRes = JSONObject(it.body()?:"").getJSONArray("results")
                   for (i in 0 until imagesRes.length()){
                       mutableListOfImages.add(imagesRes.getJSONObject(i).getJSONObject("urls").getString("regular"))
                   }

                   with(OtherImagesRecyclerView){
                       layoutManager = LinearLayoutManager(requireContext(),LinearLayoutManager.HORIZONTAL,false)
                       itemAnimator = DefaultItemAnimator()
                       adapter = CityPicturesRecyclerViewAdapter(mutableListOfImages.toList()){pic->
                           Glide.with(requireContext()).load(pic).into(CityImageView)
                       }
                   }

               }
           )

            ToursTakenTextView.setOnClickListener {
                Intent(requireContext() , ToursActivity::class.java).apply {
                    putExtra("SELECTED_TOURS_FRAGMENT","CITY_TOURS")
                    putExtra("CITY_NAME",cityNameExtra)
                }.also {
                    startActivity(it)
                }
            }

           RequestTourGuideButton.setOnClickListener {
            Intent(requireContext().applicationContext,ToursActivity::class.java).apply {
                putExtra("CITY_NAME",cityName)
                putExtra("CITY_IMAGE",cityImageExtra.toString())
                putExtra("SELECTED_TOURS_FRAGMENT","TOURS_REQUEST_FORM")
            }.also {
                startActivity(it)
            }
           }

           NavToCityButton.setOnClickListener {
               val city = cityName.substringBefore(',')
               val country = cityName.substringAfter(',').trim()
               iToursit.selectedCities.add(SimpleCityDetail(city,country, bundle.getDouble("LAT",0.0)
                   ,bundle.getDouble("LON",0.0))
               )
               iToursit.newSelectedCity=true

               parentFragmentManager.popBackStack()
               requireActivity().CustomBottomNavBar.apply {
                   visibility = View.VISIBLE
                   setItemSelected(R.id.navHomeButtonId)
               }
           }
       }
    }

    private fun fetchFireStoreData(newPic:String){
        cityRef!!.run {
            get().addOnSuccessListener {docSnap->
                if (docSnap.exists()){
                    setupPreInsertedCity(docSnap)
                }else{
                    insertNewCity(newPic)
                }
            }
        }
    }

    private fun setupPreInsertedCity(docSnap : DocumentSnapshot){
        docSnap.data!!.run {
            totalLikes = get("Liked").toString().toInt()
            LikedThisPlaceTextView.text = "$totalLikes Love This Place"
            ToursTakenTextView.text = "${get("Tours")} Tours Taken"
        }
        FirebaseObj.fireStore.collection("Users").document(FirebaseObj.uid).
        collection("Liked Cities").document(cityRef!!.id).get().addOnSuccessListener {
            isLiked = it.exists()
            favoriteImageViewID.setImageResource(if (isLiked) R.drawable.full_heart else R.drawable.heart)
            favoriteImageViewID.visibility = View.VISIBLE
        }
    }

    private fun insertNewCity(newPic: String){
        cityRef!!.run {
            set(hashMapOf(
                "Liked" to 0,
                "Tours" to 0,
                "Image" to newPic
            ))
        }
        favoriteImageViewID.visibility = View.VISIBLE
    }

    private fun onFavoriteClicked() {
        isLiked = !isLiked
        val heartDrawable = if (isLiked) R.drawable.full_heart else R.drawable.heart
        favoriteImageViewID.setImageResource(heartDrawable)

        if (isLiked) {
            addToMyFavouriteCities()
            incrementLikesOfCity()
        } else {
            removeFromMyFavoriteCities()
            decrementLikesOfCity()
        }
    }

    private fun addToMyFavouriteCities() {
        FirebaseObj.fireStore.collection("Users").document(FirebaseObj.uid).
        collection("Liked Cities").document(cityRef!!.id).set(hashMapOf(
            "Reference" to cityRef
        ))
    }

    private fun incrementLikesOfCity() {
      ++totalLikes
      cityRef!!.set(hashMapOf(
          "Liked" to totalLikes
      ), SetOptions.merge())
    }

    private fun removeFromMyFavoriteCities() {
        FirebaseObj.fireStore.collection("Users").document(FirebaseObj.uid).
        collection("Liked Cities").document(cityRef!!.id).delete()
    }

    private fun decrementLikesOfCity() {
        --totalLikes
        cityRef!!.set(hashMapOf(
            "Liked" to totalLikes
        ), SetOptions.merge())
    }
}