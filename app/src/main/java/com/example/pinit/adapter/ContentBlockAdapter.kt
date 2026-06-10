package com.example.pinit.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.example.pinit.R
import com.example.pinit.model.map.MapData
import com.example.pinit.model.post.ContentBlock
import com.example.pinit.model.Schedule

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.ArrayList

class ContentBlockAdapter(
    blocks: List<ContentBlock>?,
    private val saveListener: OnScheduleSaveListener? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // 💡 인터페이스는 오직 몇 번째 DAY 인지만 넘기도록 확실하게 1개로 통일 (자바 람다 에러 해결)
    interface OnScheduleSaveListener {
        fun onSaveClick(dayNumber: Int)
    }

    private val blocks: List<ContentBlock> = blocks ?: emptyList()

    // 💡 자바 프래그먼트에서 에러 없이 일정을 추출할 수 있도록 제공하는 안전한 public 함수
    fun getSchedulesForDay(dayNumber: Int): ArrayList<Schedule> {
        val scheduleList = ArrayList<Schedule>()
        blocks.forEach { b ->
            if (b.type == ContentBlock.TYPE_MAP) {
                val title = b.dayTitle ?: ""
                val dayNum = title.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                if (dayNum == dayNumber) {
                    b.mapData?.markers?.forEach { marker ->
                        val s = Schedule().apply {
                            this.title = marker.title ?: ""
                            this.placeName = marker.title ?: ""
                            this.latitude = marker.lat
                            this.longitude = marker.lng
                            this.color = "#FFDA44"
                            this.date = b.date ?: ""
                        }
                        scheduleList.add(s)
                    }
                }
            }
        }
        return scheduleList
    }

    companion object {
        private const val VIEW_TEXT = 0
        private const val VIEW_IMAGE = 1
        private const val VIEW_PLACE = 2
        private const val VIEW_MAP = 3
        private const val VIEW_BUDGET = 4
    }

    override fun getItemViewType(position: Int): Int =
        when (blocks[position].type) {
            ContentBlock.TYPE_IMAGE -> VIEW_IMAGE
            ContentBlock.TYPE_PLACE -> VIEW_PLACE
            ContentBlock.TYPE_MAP -> VIEW_MAP
            ContentBlock.TYPE_BUDGET -> VIEW_BUDGET
            else -> VIEW_TEXT
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_IMAGE -> ImageVH(inflater.inflate(R.layout.item_block_image, parent, false))
            VIEW_PLACE -> PlaceVH(inflater.inflate(R.layout.item_block_place, parent, false))
            VIEW_MAP -> MapVH(inflater.inflate(R.layout.item_block_map, parent, false))
            VIEW_BUDGET -> BudgetVH(inflater.inflate(R.layout.item_block_budget, parent, false))
            else -> TextVH(inflater.inflate(R.layout.item_block_text, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val block = blocks[position]
        when (holder) {
            is TextVH -> holder.bind(block)
            is ImageVH -> holder.bind(block)
            is PlaceVH -> holder.bind(block)
            is MapVH -> holder.bind(block, saveListener)
            is BudgetVH -> holder.bind(block)
        }
    }

    override fun getItemCount(): Int = blocks.size

    class TextVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvBlockText)
        fun bind(b: ContentBlock) { tvText.text = b.textContent ?: "" }
    }

    class ImageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivBlockImage)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvBlockCaption)
        fun bind(b: ContentBlock) {
            Glide.with(ivImage.context).load(b.imageUrl ?: "").into(ivImage)
            if ((b.textContent ?: "").isEmpty()) {
                tvCaption.visibility = View.GONE
            } else {
                tvCaption.visibility = View.VISIBLE
                tvCaption.text = b.textContent
            }
        }
    }

    class PlaceVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBlockPlaceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvBlockPlaceAddress)
        fun bind(b: ContentBlock) {
            tvName.text = b.placeName ?: ""
            if ((b.placeAddress ?: "").isEmpty()) {
                tvAddress.visibility = View.GONE
            } else {
                tvAddress.visibility = View.VISIBLE
                tvAddress.text = b.placeAddress
            }
        }
    }

    class MapVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayTitle: TextView = itemView.findViewById(R.id.tvBlockMapDayTitle)
        private val layoutSummary: LinearLayout = itemView.findViewById(R.id.layoutBlockMapSummary)
        private val btnReadMore: TextView? = itemView.findViewById(R.id.btnBlockMapReadMore)
        private val mapView: MapView? = itemView.findViewById(R.id.blockMapView)
        private val btnSaveSingleDay: TextView? = itemView.findViewById(R.id.btnSaveSingleDay)

        fun bind(b: ContentBlock, listener: OnScheduleSaveListener?) {
            val titleText = b.dayTitle ?: ""
            val dateText = b.date ?: ""
            tvDayTitle.text = if (titleText.isNotEmpty()) "$titleText ($dateText)" else dateText

            btnSaveSingleDay?.setOnClickListener {
                val dayNum = titleText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 1
                listener?.onSaveClick(dayNum)
            }

            val markers = b.mapData?.markers ?: emptyList()
            layoutSummary.removeAllViews()
            markers.forEachIndexed { index, m ->
                val tv = TextView(layoutSummary.context).apply {
                    text = "${index + 1}. ${m.title ?: ""}"
                    setTextColor(0xFF222222.toInt())
                    setPadding(0, 10, 0, 10)
                    textSize = 15f
                }
                layoutSummary.addView(tv)
            }

            btnReadMore?.setOnClickListener {
                if (layoutSummary.visibility == View.VISIBLE) {
                    layoutSummary.visibility = View.GONE
                    btnReadMore.text = "더보기 ▼"
                } else {
                    layoutSummary.visibility = View.VISIBLE
                    btnReadMore.text = "접기 ▲"
                }
            }

            mapView?.apply {
                onCreate(null)
                onResume()
                getMapAsync { googleMap ->
                    googleMap.uiSettings.setAllGesturesEnabled(false)
                    drawMapData(googleMap, b.mapData)
                }
            }
        }
    }

    class BudgetVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTotal: TextView = itemView.findViewById(R.id.tvBlockBudgetTotal)
        private val tvFood: TextView = itemView.findViewById(R.id.tvBlockBudgetFood)
        private val tvTransport: TextView = itemView.findViewById(R.id.tvBlockBudgetTransport)
        private val tvAccom: TextView = itemView.findViewById(R.id.tvBlockBudgetAccom)
        private val tvShopping: TextView = itemView.findViewById(R.id.tvBlockBudgetShopping)
        private val tvSightseeing: TextView = itemView.findViewById(R.id.tvBlockBudgetSightseeing)
        private val tvEtc: TextView = itemView.findViewById(R.id.tvBlockBudgetEtc)
        fun bind(b: ContentBlock) {
            val total = b.budgetFood + b.budgetTransport + b.budgetAccom +
                    b.budgetShopping + b.budgetSightseeing + b.budgetEtc
            tvTotal.text = "총 ${total}만원"
            tvFood.text = b.budgetFood.toString()
            tvTransport.text = b.budgetTransport.toString()
            tvAccom.text = b.budgetAccom.toString()
            tvShopping.text = b.budgetShopping.toString()
            tvSightseeing.text = b.budgetSightseeing.toString()
            tvEtc.text = b.budgetEtc.toString()
        }
    }
}

private fun drawMapData(googleMap: GoogleMap, mapData: MapData?) {
    val markers = mapData?.markers ?: emptyList()
    if (markers.isEmpty()) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.5665, 126.9780), 10f))
        return
    }
    val positions = markers.map { LatLng(it.lat, it.lng) }
    markers.forEachIndexed { i, m ->
        googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(m.lat, m.lng))
                .title(if ((m.title ?: "").isNotEmpty()) "${i + 1}. ${m.title}" else "${i + 1}")
                .snippet(m.description ?: "")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
    }
    if (positions.size >= 2) {
        googleMap.addPolyline(
            PolylineOptions().addAll(positions).width(8f)
                .color(Color.parseColor("#FF6B35")).geodesic(true)
        )
        val builder = LatLngBounds.Builder()
        positions.forEach { builder.include(it) }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
    } else {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(positions[0], 15f))
    }
}