package com.example.pinit.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.example.pinit.R
import com.example.pinit.model.map.MapData
import com.example.pinit.model.post.ContentBlock

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

/**
 * 게시글 본문 블록 렌더링 (Notion 스타일).
 * 타입: text | image | place | map
 *
 * DAY 는 작성 시 map → place → text/image → place ... 순으로 펼쳐져 저장되므로
 * 여기서는 각 블록을 순서대로 그리기만 하면 된다.
 *
 * map 블록은 MapData.markers 의 저장된 좌표로 즉시 핀/동선을 그린다(geocode 불필요).
 */
class ContentBlockAdapter(
    blocks: List<ContentBlock>?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val blocks: List<ContentBlock> = blocks ?: emptyList()

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
            is MapVH -> holder.bind(block)
            is BudgetVH -> holder.bind(block)
        }
    }

    override fun getItemCount(): Int = blocks.size

    // =====================================================
    // ViewHolders
    // =====================================================

    class TextVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(R.id.tvBlockText)
        fun bind(b: ContentBlock) { tvText.text = b.textContent }
    }

    class ImageVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImage: ImageView = itemView.findViewById(R.id.ivBlockImage)
        private val tvCaption: TextView = itemView.findViewById(R.id.tvBlockCaption)
        fun bind(b: ContentBlock) {
            Glide.with(ivImage.context).load(b.imageUrl).into(ivImage)
            if (b.textContent.isEmpty()) {
                tvCaption.visibility = View.GONE
            } else {
                tvCaption.visibility = View.VISIBLE
                tvCaption.text = b.textContent
            }
        }
    }

    /** place 블록: 핀 아이콘 + 장소명 (+ 주소) */
    class PlaceVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvBlockPlaceName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvBlockPlaceAddress)
        fun bind(b: ContentBlock) {
            tvName.text = b.placeName
            if (b.placeAddress.isEmpty()) {
                tvAddress.visibility = View.GONE
            } else {
                tvAddress.visibility = View.VISIBLE
                tvAddress.text = b.placeAddress
            }
        }
    }

    /** map 블록: DAY 제목 + 지도(동선+핀) + 요약리스트(①②③ + 더보기) */
    class MapVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayTitle: TextView = itemView.findViewById(R.id.tvBlockMapDayTitle)
        private val layoutSummary: LinearLayout = itemView.findViewById(R.id.layoutBlockMapSummary)
        private val btnReadMore: TextView? = itemView.findViewById(R.id.btnBlockMapReadMore)
        private val mapView: MapView? = itemView.findViewById(R.id.blockMapView)
        fun bind(b: ContentBlock) {
            tvDayTitle.text = if (b.dayTitle.isNotEmpty()) "${b.dayTitle} (${b.date})" else b.date

            // 요약 리스트: markers 의 title 을 순서대로
            val markers = b.mapData?.markers ?: emptyList()
            layoutSummary.removeAllViews()
            markers.forEachIndexed { index, m ->
                val tv = TextView(layoutSummary.context).apply {
                    text = "${index + 1}. ${m.title}"
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

    /** budget 블록: 항목별 예산 + 총합 (만원 단위) */
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

// =====================================================
// MapData(markers 좌표) -> 핀/동선 그리기 (저장 좌표 사용, geocode 불필요)
// =====================================================
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
                .title(if (m.title.isNotEmpty()) "${i + 1}. ${m.title}" else "${i + 1}")
                .snippet(m.description)
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