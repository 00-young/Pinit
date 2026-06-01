package com.example.pinit.fragment

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

import com.example.pinit.R
import com.example.pinit.activity.PlaceSearchActivity
import com.example.pinit.activity.PostTravelSettingActivity
import com.example.pinit.database.PlacesApiHelper
import com.example.pinit.model.DailySchedule
import com.example.pinit.model.MyPlan
import com.example.pinit.model.map.MapData
import com.example.pinit.model.map.MapMarker
import com.example.pinit.model.post.ContentBlock
import com.example.pinit.model.post.EditorBlock
import com.example.pinit.model.post.Post
import com.example.pinit.repository.PostRepository

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

import org.json.JSONObject

import java.net.URLEncoder
import java.util.UUID

import okhttp3.OkHttpClient
import okhttp3.Request

class CreatePostFragment : Fragment() {

    private val apiKey = PlacesApiHelper.API_KEY

    private lateinit var travelSettingLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<androidx.activity.result.PickVisualMediaRequest>
    private lateinit var placeSearchLauncher: ActivityResultLauncher<Intent>

    private var layoutDynamicContent: LinearLayout? = null
    private var layoutImportedBudget: LinearLayout? = null
    private var layoutTagsContainer: LinearLayout? = null
    private var layoutTravelSettingTagsContainer: LinearLayout? = null
    private var ivSelectedPhoto: ImageView? = null

    private lateinit var tvTotalBudget: TextView
    private lateinit var etBudgetFood: EditText
    private lateinit var etBudgetTransport: EditText
    private lateinit var etBudgetAccom: EditText
    private lateinit var etBudgetShopping: EditText
    private lateinit var etBudgetSightseeing: EditText
    private lateinit var etBudgetEtc: EditText

    private fun interface GeocodeCallback {
        fun onResult(latLng: LatLng?)
    }

    private lateinit var etPostTitle: EditText
    private lateinit var btnUpload: Button

    private val postRepository = PostRepository()

    private var thumbnailUri: Uri? = null

    // 마지막으로 포커스된 본문 텍스트 블록 (이 블록 뒤에 이미지/일정을 삽입)
    private var lastFocusedBlock: View? = null

    // 업로드 진행 중 여부 (중복 업로드 방지 + 버튼 비활성화)
    private var isUploading = false

    private data class PendingImage(val editor: EditorBlock, val uri: Uri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        travelSettingLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data!!
                layoutTravelSettingTagsContainer?.let { container ->
                    container.removeAllViews()
                    addTravelSettingTag(data.getStringExtra("selectedDate"))
                    addTravelSettingTag(data.getStringExtra("selectedCountry"))
                    addTravelSettingTag(data.getStringExtra("selectedPeople"))
                }
            }
        }

        // 갤러리(Photo Picker) -> 이미지 블록. 권한 없이 사진만 선택 가능.
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            // 사용자가 선택 안 하면 uri 가 null
            uri?.let { insertImageBlock(it) }
        }

        // 장소 단건 -> place 블록 (단독 장소)
        placeSearchLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val placeName = result.data!!.getStringExtra("selectedPlaceName")
                val placeAddress = result.data!!.getStringExtra("selectedPlaceAddress")
                if (placeName != null) insertPlaceBlock(placeName, placeAddress)
            }
        }

        // 내 일정 불러오기 -> DAY 를 map + place 블록들로 펼침 (기존 글 유지, 커서 위치에 삽입)
        parentFragmentManager.setFragmentResultListener("planResult", this) { _, bundle ->
            val selectedPlan = getSerializableCompat(bundle, "selectedPlan", MyPlan::class.java)
            if (selectedPlan != null && layoutDynamicContent != null) {
                selectedPlan.schedules?.forEach { expandDayIntoBlocks(it) }
            }
        }

        parentFragmentManager.setFragmentResultListener("tagResult", this) { _, bundle ->
            val selectedTags = bundle.getStringArrayList("selectedTags")
            if (selectedTags != null && layoutTagsContainer != null) {
                layoutTagsContainer!!.removeAllViews()
                for (tag in selectedTags) {
                    val tvTag = TextView(context).apply {
                        text = "#$tag"
                        setTextColor(0xFF000000.toInt())
                        setBackgroundColor(0xFFEEEEEE.toInt())
                        setPadding(32, 12, 32, 12)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, 0, 16, 0) }
                    }
                    layoutTagsContainer!!.addView(tvTag)
                }
            }
        }

        parentFragmentManager.setFragmentResultListener("budgetResult", this) { _, bundle ->
            layoutImportedBudget?.let {
                it.visibility = View.VISIBLE
                etBudgetFood.setText(bundle.getInt("budgetFood", 0).toString())
                etBudgetTransport.setText(bundle.getInt("budgetTransport", 0).toString())
                etBudgetAccom.setText(bundle.getInt("budgetAccom", 0).toString())
                etBudgetShopping.setText(bundle.getInt("budgetShopping", 0).toString())
                etBudgetSightseeing.setText(bundle.getInt("budgetSightseeing", 0).toString())
                etBudgetEtc.setText(bundle.getInt("budgetEtc", 0).toString())
                calculateTotalBudget()
            }
        }
    }

    // =====================================================
    // 블록 삽입 (각 뷰 tag 에 EditorBlock)
    // =====================================================

    /** 이미지 블록 */
    private fun insertImageBlock(imageUri: Uri) {
        val container = layoutDynamicContent ?: return

        val editor = EditorBlock(
            id = UUID.randomUUID().toString(),
            type = ContentBlock.TYPE_IMAGE,
            localImageUri = imageUri
        )
        val iv = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)
            ).apply { setMargins(0, 16, 0, 16) }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageURI(imageUri)
            tag = editor
        }
        // 커서(마지막 포커스 블록) 다음에 삽입
        addBlockAtCursor(iv)

        // 첫 이미지를 대표 이미지(썸네일)로 지정.
        if (thumbnailUri == null) {
            thumbnailUri = imageUri
        }
        // 이미지 바로 뒤에 이어 쓸 텍스트 블록
        addCommentBlockAtCursor("내용을 입력하세요")
    }

    /** 단독 place 블록 (장소 검색으로 직접 추가) */
    private fun insertPlaceBlock(placeName: String, placeAddress: String?) {
        val editor = EditorBlock(
            id = UUID.randomUUID().toString(),
            type = ContentBlock.TYPE_PLACE,
            placeName = placeName,
            placeAddress = placeAddress ?: ""
        )
        val placeView = buildPlaceHeaderView(placeName, placeAddress, editor)
        addBlockAtCursor(placeView)
        addCommentBlockAtCursor("이 장소에 대한 이야기를 적어보세요...")
    }

    /** DAY 하나를 map 블록 + place 블록들로 펼침 (커서 위치에 차례로 삽입) */
    private fun expandDayIntoBlocks(day: DailySchedule) {
        // 1) map 블록: 지도 + 요약리스트 + 날짜 (담기용 데이터가 여기 모두 모임)
        insertMapBlock(day)

        // 2) 장소마다 place 블록 + 코멘트 텍스트 블록
        day.places.forEachIndexed { index, placeName ->
            val editor = EditorBlock(
                id = UUID.randomUUID().toString(),
                type = ContentBlock.TYPE_PLACE,
                placeName = placeName
            )
            val placeView = buildPlaceHeaderView(placeName, null, editor)
            addBlockAtCursor(placeView)
            addCommentBlockAtCursor("${index + 1}. $placeName 에 대한 이야기를 적어보세요...")
        }
    }

    /** map 블록: 지도(동선+핀) + 요약 리스트(①②③ + 더보기). MapData 좌표를 미리 채운다. */
    private fun insertMapBlock(day: DailySchedule) {
        val container = layoutDynamicContent ?: return

        val mapBlockView = LayoutInflater.from(context)
            .inflate(R.layout.item_block_map, container, false)
        val tvDayTitle = mapBlockView.findViewById<TextView>(R.id.tvBlockMapDayTitle)
        val mapView = mapBlockView.findViewById<MapView>(R.id.blockMapView)
        val layoutSummary = mapBlockView.findViewById<LinearLayout>(R.id.layoutBlockMapSummary)
        val btnReadMore = mapBlockView.findViewById<Button?>(R.id.btnBlockMapReadMore)

        tvDayTitle.text = "${day.dayTitle} (${day.date})"

        // 요약 리스트(①②③)를 markers/places 순서대로 채움
        layoutSummary.removeAllViews()
        day.places.forEachIndexed { index, name ->
            val tv = TextView(layoutSummary.context).apply {
                text = "${index + 1}. $name"
                setTextColor(0xFF222222.toInt())
                setPadding(0, 10, 0, 10)
                textSize = 15f
            }
            layoutSummary.addView(tv)
        }

        // 더보기/접기: 처음엔 펼친 상태(피그마처럼 일부만 보이게 하려면 초기 GONE 후 토글)
        btnReadMore?.setOnClickListener {
            if (layoutSummary.visibility == View.VISIBLE) {
                layoutSummary.visibility = View.GONE
                btnReadMore.text = "더보기 ▼"
            } else {
                layoutSummary.visibility = View.VISIBLE
                btnReadMore.text = "접기 ▲"
            }
        }

        val editor = EditorBlock(
            id = UUID.randomUUID().toString(),
            type = ContentBlock.TYPE_MAP,
            date = day.date,
            dayTitle = day.dayTitle,
            mapData = MapData()   // 좌표는 아래 geocode 로 채움
        )

        mapView?.apply {
            onCreate(null)
            onResume()
            getMapAsync { googleMap ->
                googleMap.uiSettings.setAllGesturesEnabled(false)
                buildMapDataFromPlaces(googleMap, day.places) { built ->
                    editor.mapData = built   // 저장용 좌표 보강
                }
            }
        }

        mapBlockView.tag = editor
        addBlockAtCursor(mapBlockView)
    }

    /** place 헤더 뷰 (핀 아이콘 + 장소명 + 주소) */
    private fun buildPlaceHeaderView(
        placeName: String,
        placeAddress: String?,
        editor: EditorBlock
    ): View {
        val container = layoutDynamicContent!!
        val placeView = LayoutInflater.from(context)
            .inflate(R.layout.item_block_place, container, false)
        val tvName = placeView.findViewById<TextView>(R.id.tvBlockPlaceName)
        val tvAddress = placeView.findViewById<TextView>(R.id.tvBlockPlaceAddress)
        tvName.text = placeName
        if (placeAddress.isNullOrEmpty()) {
            tvAddress.visibility = View.GONE
        } else {
            tvAddress.visibility = View.VISIBLE
            tvAddress.text = placeAddress
        }
        placeView.tag = editor
        return placeView
    }

    /** 다른 블록 사이의 코멘트(텍스트) 블록 */
    private fun addCommentBlock(hint: String) {
        addCommentBlockInternal(hint, atCursor = false)
    }

    /** 코멘트 블록을 커서(마지막 포커스 블록) 다음에 삽입 */
    private fun addCommentBlockAtCursor(hint: String) {
        addCommentBlockInternal(hint, atCursor = true)
    }

    private fun addCommentBlockInternal(hint: String, atCursor: Boolean) {
        val container = layoutDynamicContent ?: return
        val editor = EditorBlock(id = UUID.randomUUID().toString(), type = ContentBlock.TYPE_TEXT)
        val et = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 24) }
            this.hint = hint
            setBackgroundColor(Color.TRANSPARENT)
            gravity = Gravity.TOP
            minLines = 2
            tag = editor
            setOnFocusChangeListener { v, hasFocus -> if (hasFocus) lastFocusedBlock = v }
        }
        if (atCursor) addBlockAtCursor(et) else container.addView(et)
    }

    // =====================================================
    // 지도 로직
    // =====================================================

    /** 장소명 리스트 -> 좌표 구해 핀/동선 그리고, 완성된 MapData 콜백 */
    private fun buildMapDataFromPlaces(
        googleMap: GoogleMap?,
        places: List<String>?,
        onBuilt: (MapData) -> Unit
    ) {
        if (googleMap == null || places.isNullOrEmpty()) return

        val total = places.size
        val ordered = arrayOfNulls<LatLng>(total)
        val done = intArrayOf(0)

        for (i in 0 until total) {
            val placeName = places[i]
            geocode(placeName) { latLng ->
                activity?.runOnUiThread {
                    if (latLng != null) {
                        ordered[i] = latLng
                        googleMap.addMarker(
                            MarkerOptions()
                                .position(latLng)
                                .title("${i + 1}. $placeName")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        )
                    }
                    done[0]++
                    if (done[0] == total) {
                        val valid = ordered.filterNotNull()
                        if (valid.size >= 2) {
                            googleMap.addPolyline(
                                PolylineOptions().addAll(valid).width(8f)
                                    .color(Color.parseColor("#FF6B35")).geodesic(true)
                            )
                        }
                        fitCameraToPins(googleMap, valid)
                        val markers = places.mapIndexedNotNull { idx, name ->
                            ordered[idx]?.let { MapMarker(it.latitude, it.longitude, name, "") }
                        }
                        onBuilt(MapData(polyline = "", markers = markers))
                    }
                }
            }
        }
    }

    private fun fitCameraToPins(googleMap: GoogleMap?, positions: List<LatLng>) {
        if (googleMap == null) return
        when {
            positions.isEmpty() ->
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(37.5665, 126.9780),
                        10f
                    )
                )

            positions.size == 1 ->
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(positions[0], 15f))

            else -> {
                val builder = LatLngBounds.Builder()
                positions.forEach { builder.include(it) }
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120))
            }
        }
    }

    private fun geocode(address: String, callback: GeocodeCallback) {
        Thread {
            try {
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=" +
                        URLEncoder.encode(address, "UTF-8") +
                        "&language=ko&key=$apiKey"
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                val results = JSONObject(body).optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val loc = results.getJSONObject(0)
                        .getJSONObject("geometry").getJSONObject("location")
                    callback.onResult(LatLng(loc.getDouble("lat"), loc.getDouble("lng")))
                } else {
                    callback.onResult(null)
                }
            } catch (e: Exception) {
                callback.onResult(null)
            }
        }.start()
    }

    // =====================================================
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create_post, container, false)

        layoutDynamicContent = view.findViewById(R.id.layoutDynamicContent)
        layoutImportedBudget = view.findViewById(R.id.layoutImportedBudget)
        layoutTagsContainer = view.findViewById(R.id.layoutTagsContainer)
        layoutTravelSettingTagsContainer = view.findViewById(R.id.layoutTravelSettingTagsContainer)
        ivSelectedPhoto = view.findViewById(R.id.ivSelectedPhoto)

        tvTotalBudget = view.findViewById(R.id.tvTotalBudget)
        etBudgetAccom = view.findViewById(R.id.etBudgetAccom)
        etBudgetTransport = view.findViewById(R.id.etBudgetTransport)
        etBudgetFood = view.findViewById(R.id.etBudgetFood)
        etBudgetShopping = view.findViewById(R.id.etBudgetShopping)
        etBudgetSightseeing = view.findViewById(R.id.etBudgetSightseeing)
        etBudgetEtc = view.findViewById(R.id.etBudgetEtc)

        etPostTitle = view.findViewById(R.id.etPostTitle)
        btnUpload = view.findViewById(R.id.btnRegister)
        btnUpload.setOnClickListener { uploadPost() }

        val budgetWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateTotalBudget()
            }
        }
        etBudgetFood.addTextChangedListener(budgetWatcher)
        etBudgetTransport.addTextChangedListener(budgetWatcher)
        etBudgetAccom.addTextChangedListener(budgetWatcher)
        etBudgetShopping.addTextChangedListener(budgetWatcher)
        etBudgetSightseeing.addTextChangedListener(budgetWatcher)
        etBudgetEtc.addTextChangedListener(budgetWatcher)

        val spinnerVisibility = view.findViewById<Spinner>(R.id.spinnerVisibility)
        val visibilityItems = arrayOf("전체공개", "나만보기")
        val spinnerAdapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, visibilityItems
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerVisibility.adapter = spinnerAdapter

        view.findViewById<View>(R.id.btnTravelSetting).setOnClickListener {
            travelSettingLauncher.launch(Intent(activity, PostTravelSettingActivity::class.java))
        }
        view.findViewById<View>(R.id.btnLoadBudget).setOnClickListener {
            BudgetBottomSheetFragment().show(parentFragmentManager, "BudgetBottomSheet")
        }
        view.findViewById<View>(R.id.btnLoadMyPlan).setOnClickListener {
            MyPlansBottomSheetFragment().show(parentFragmentManager, "MyPlansBottomSheet")
        }
        view.findViewById<View>(R.id.btnInsertTag).setOnClickListener {
            TagBottomSheetFragment().show(parentFragmentManager, "TagBottomSheet")
        }
        view.findViewById<View>(R.id.ivMenuPhoto).setOnClickListener {
            galleryLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        view.findViewById<View>(R.id.ivMenuLocation).setOnClickListener {
            val intent = Intent(activity, PlaceSearchActivity::class.java)
            intent.putExtra("isPickingMode", true)
            placeSearchLauncher.launch(intent)
        }

        // 초기 빈 텍스트 블록은 컨테이너가 비어있을 때만 (뷰 재생성 시 중복 방지)
        if (layoutDynamicContent?.childCount == 0) {
            addTextBlock()
        }
        return view
    }

    // =====================================================
    // 업로드
    // =====================================================
    private fun uploadPost() {
        val title = etPostTitle.text.toString().trim()
        if (title.isEmpty()) {
            toast("게시글 제목을 입력하세요"); return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        if (uid.isEmpty()) {
            toast("로그인이 필요합니다"); return
        }

        // 중복 업로드 방지: 이미 업로드 중이면 무시
        if (isUploading) return

        val postId = UUID.randomUUID().toString()
        val post = Post(
            postId = postId,
            userId = uid,
            userNickname = "user",
            title = title,
            postImageType = "image",
            thumbnailImageUrl = "",
            hashtags = listOf("여행"),
            createdAt = Timestamp.now()
        )

        val editors = mutableListOf<EditorBlock>()
        val pendingImages = mutableListOf<PendingImage>()
        var order = 0

        val content = layoutDynamicContent ?: return
        for (i in 0 until content.childCount) {
            val editor = content.getChildAt(i).tag as? EditorBlock ?: continue
            when (editor.type) {
                ContentBlock.TYPE_TEXT -> {
                    val v = content.getChildAt(i)
                    if (v is EditText) editor.text = v.text.toString().trim()
                    if (editor.text.isEmpty()) continue
                }

                ContentBlock.TYPE_IMAGE -> {
                    editor.localImageUri?.let { pendingImages.add(PendingImage(editor, it)) }
                }
                // place / map 은 데이터가 이미 채워져 있음
            }
            editor.sortOrder = order++
            editors.add(editor)
        }

        // 갤러리 썸네일이 있으면 그대로 업로드
        if (thumbnailUri != null) {
            setUploading(true)
            uploadImagesThenSave(post, editors, pendingImages, null)
            return
        }

        // 썸네일이 없으면 모든 DAY 핀을 합친 Static Map 으로 대체.
        // 좌표가 아직 안 채워진 map 블록이 있으면 먼저 geocode 로 보강한다.
        val mapEditors = editors.filter { it.type == ContentBlock.TYPE_MAP }

        if (mapEditors.isEmpty()) {
            // 일정(map 블록)도 없고 썸네일도 없음 → 만들 수 없음
            toast("대표 이미지를 선택하거나 일정을 추가하세요")
            return
        }

        // 업로드 시작(버튼 회색 잠금). geocode 동안에도 막힌 상태 유지.
        setUploading(true)

        // 좌표 미확보 map 블록을 채운 뒤 업로드 진행
        ensureMapCoordinates(mapEditors) {
            val staticMapUrl = buildStaticMapUrl(editors)
            if (staticMapUrl == null) {
                // geocode 후에도 좌표가 하나도 안 잡힌 경우
                setUploading(false)
                toast("지도를 만들 수 없습니다. 대표 이미지를 선택해주세요")
                return@ensureMapCoordinates
            }
            uploadImagesThenSave(post, editors, pendingImages, staticMapUrl)
        }
    }

    /**
     * map 블록들 중 좌표(markers)가 비어있는 것을 geocode 로 채운 뒤 onReady 호출.
     * 모든 보강이 끝나면(또는 보강할 게 없으면) 메인 스레드에서 onReady 를 부른다.
     */
    private fun ensureMapCoordinates(mapEditors: List<EditorBlock>, onReady: () -> Unit) {
        // 좌표가 비어있어 보강이 필요한 (블록, 장소명) 목록 수집
        data class Need(val editor: EditorBlock, val placeNames: List<String>)

        val needs = mapEditors.mapNotNull { editor ->
            val markers = editor.mapData?.markers ?: emptyList()
            val hasCoords = markers.any { it.lat != 0.0 || it.lng != 0.0 }
            if (hasCoords) {
                null   // 이미 좌표 있음 → 보강 불필요
            } else {
                val names = markers.map { it.title }.filter { it.isNotEmpty() }
                if (names.isEmpty()) null else Need(editor, names)
            }
        }

        if (needs.isEmpty()) {
            onReady()   // 보강할 것 없음 → 바로 진행
            return
        }

        // 전체 블록 완료를 세는 카운터
        val blocksRemaining = intArrayOf(needs.size)

        for (need in needs) {
            val resolved = arrayOfNulls<MapMarker>(need.placeNames.size)
            val placeDone = intArrayOf(0)

            for (idx in need.placeNames.indices) {
                val name = need.placeNames[idx]
                geocode(name) { latLng ->
                    activity?.runOnUiThread {
                        if (latLng != null) {
                            resolved[idx] = MapMarker(latLng.latitude, latLng.longitude, name, "")
                        }
                        placeDone[0]++

                        // 이 블록의 모든 장소 geocode 완료 → mapData 갱신
                        if (placeDone[0] == need.placeNames.size) {
                            val markers = resolved.filterNotNull()
                            if (markers.isNotEmpty()) {
                                need.editor.mapData = MapData(polyline = "", markers = markers)
                            }
                            // 블록 하나 완료 → 전체 완료 시 진행
                            blocksRemaining[0]--
                            if (blocksRemaining[0] == 0) onReady()
                        }
                    }
                }
            }
        }
    }

    private fun uploadImagesThenSave(
        post: Post,
        editors: List<EditorBlock>,
        pending: List<PendingImage>,
        staticMapUrl: String?
    ) {
        if (pending.isEmpty()) {
            savePost(post, editors, staticMapUrl); return
        }

        val storage = FirebaseStorage.getInstance()
        val remaining = intArrayOf(pending.size)
        val failed = booleanArrayOf(false)

        for (p in pending) {
            val ref = storage.reference
                .child("posts/${post.postId}/blocks/${p.editor.id}.jpg")
            ref.putFile(p.uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) throw task.exception!!
                    ref.downloadUrl
                }
                .addOnSuccessListener { downloadUri ->
                    p.editor.imageUrl = downloadUri.toString()
                    remaining[0]--
                    if (remaining[0] == 0 && !failed[0]) savePost(post, editors, staticMapUrl)
                }
                .addOnFailureListener {
                    if (!failed[0]) {
                        failed[0] = true
                        setUploading(false)
                        toast("이미지 업로드 실패")
                    }
                }
        }
    }

    /**
     * @param staticMapUrl 썸네일 미설정 시 사용할 Static Map URL (있으면 이걸로 저장).
     */
    private fun savePost(post: Post, editors: List<EditorBlock>, staticMapUrl: String?) {
        val blocks = editors.map { ContentBlock.from(it) }

        // 썸네일 결정: 갤러리 이미지(Uri)가 있으면 repository 가 Storage 업로드,
        // 없으면 staticMapUrl 을 thumbnailImageUrl 에 직접 넣어 저장.
        if (thumbnailUri != null) {
            postRepository.uploadPost(
                post, thumbnailUri!!, blocks,
                {
                    toast("업로드 성공")
                    requireActivity().supportFragmentManager.popBackStack()
                    null
                },
                {
                    setUploading(false)
                    toast("업로드 실패")
                    null
                }
            )
        } else {
            // 지도 썸네일: Uri 업로드 없이 URL 을 넣은 Post 저장
            val postWithMap = post.copy(
                thumbnailImageUrl = staticMapUrl ?: "",
                postImageType = "map"
            )
            postRepository.uploadPostWithoutThumbnail(
                postWithMap, blocks,
                {
                    toast("업로드 성공")
                    requireActivity().supportFragmentManager.popBackStack()
                    null
                },
                {
                    setUploading(false)
                    toast("업로드 실패")
                    null
                }
            )
        }
    }

    // =====================================================
    // 헬퍼
    // =====================================================
    private fun addTravelSettingTag(text: String?) {
        if (text == null || text.trim().isEmpty() || text == "날짜를 선택하세요") return
        val tvTag = TextView(context).apply {
            this.text = text
            setTextColor(0xFF333333.toInt())
            background = GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                setStroke(2, 0xFFDDDDDD.toInt())
                cornerRadius = 40f
            }
            setPadding(32, 12, 32, 12)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 16, 0) }
        }
        layoutTravelSettingTagsContainer?.addView(tvTag)
    }

    private fun calculateTotalBudget() {
        val total = parseBudgetNumber(etBudgetFood.text.toString()) +
                parseBudgetNumber(etBudgetTransport.text.toString()) +
                parseBudgetNumber(etBudgetAccom.text.toString()) +
                parseBudgetNumber(etBudgetShopping.text.toString()) +
                parseBudgetNumber(etBudgetSightseeing.text.toString()) +
                parseBudgetNumber(etBudgetEtc.text.toString())
        tvTotalBudget.text = "총 ${total}만원"
    }

    private fun parseBudgetNumber(text: String?): Int =
        try {
            if (text.isNullOrBlank()) 0 else text.trim().toInt()
        } catch (e: NumberFormatException) {
            0
        }

    private fun addTextBlock() {
        val container = layoutDynamicContent ?: return
        val editor = EditorBlock(id = UUID.randomUUID().toString(), type = ContentBlock.TYPE_TEXT)
        val editText = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 24, 0, 24) }
            hint = "내용을 입력하세요"
            setBackgroundColor(Color.TRANSPARENT)
            minLines = 3
            tag = editor
            // 이 블록에 포커스가 가면 삽입 기준 위치로 기억
            setOnFocusChangeListener { v, hasFocus -> if (hasFocus) lastFocusedBlock = v }
        }
        container.addView(editText)
    }

    private fun dp(value: Int): Int = Math.round(value * resources.displayMetrics.density)

    /**
     * 본문에 블록을 삽입할 위치(인덱스)를 반환한다.
     * - 마지막으로 포커스된 블록이 있으면 그 "다음" 위치
     * - 없으면 맨 끝
     */
    private fun insertIndex(): Int {
        val container = layoutDynamicContent ?: return 0
        val focused = lastFocusedBlock
        if (focused != null) {
            val idx = container.indexOfChild(focused)
            if (idx >= 0) return idx + 1
        }
        return container.childCount
    }

    /** 본문 컨테이너의 지정 위치에 뷰를 삽입 (포커스 블록 다음). 삽입 후 위치를 한 칸 밀어준다. */
    private fun addBlockAtCursor(viewToAdd: View) {
        val container = layoutDynamicContent ?: return
        val index = insertIndex()
        container.addView(viewToAdd, index)
        // 다음 삽입이 방금 넣은 블록 뒤로 이어지도록 기준 갱신
        lastFocusedBlock = viewToAdd
    }

    /**
     * 업로드 상태 전환.
     * - 업로드 중: 버튼 비활성화 + 회색 + "업로드 중..." 텍스트
     * - 완료/실패: 버튼 복구
     */
    private fun setUploading(uploading: Boolean) {
        isUploading = uploading
        btnUpload.isEnabled = !uploading
        if (uploading) {
            btnUpload.text = "업로드 중..."
            btnUpload.setBackgroundColor(0xFFCCCCCC.toInt())   // 회색
            btnUpload.setTextColor(0xFF888888.toInt())
        } else {
            btnUpload.text = "등록하기"
            // 원래 배경/글자색으로 복구 (프로젝트 기본 버튼색에 맞게 조정)
            btnUpload.setBackgroundColor(0xFFFF6B35.toInt())
            btnUpload.setTextColor(0xFFFFFFFF.toInt())
        }
    }

    /**
     * 모든 map 블록의 마커를 합쳐 Static Maps API URL 을 만든다.
     * 썸네일 미설정 시 게시글 대표 이미지로 사용.
     * 마커가 하나도 없으면 null 반환.
     *
     * ※ 사용 중인 API 키에 "Maps Static API" 가 활성화돼 있어야 한다.
     */
    private fun buildStaticMapUrl(editors: List<EditorBlock>): String? {
        // 모든 map 블록의 마커를 방문 순서대로 평탄화
        val allMarkers = editors
            .filter { it.type == ContentBlock.TYPE_MAP }
            .mapNotNull { it.mapData?.markers }
            .flatten()
            .filter { it.lat != 0.0 || it.lng != 0.0 }   // 좌표 있는 것만

        if (allMarkers.isEmpty()) return null

        val sb = StringBuilder("https://maps.googleapis.com/maps/api/staticmap?")
        sb.append("size=600x400")          // 썸네일 크기
        sb.append("&scale=2")              // 고해상도
        sb.append("&maptype=roadmap")

        // 마커: 번호 라벨(1~9) + 좌표
        allMarkers.forEachIndexed { index, m ->
            val label = if (index < 9) "${index + 1}" else ""   // Static Maps 라벨은 0-9, A-Z 만
            sb.append("&markers=")
            if (label.isNotEmpty()) sb.append("label:$label%7C")
            sb.append("color:0xFF6B35%7C")
            sb.append("${m.lat},${m.lng}")
        }

        // 동선: 마커가 2개 이상이면 경로(path) 추가
        if (allMarkers.size >= 2) {
            sb.append("&path=color:0xFF6B35FF%7Cweight:4")
            allMarkers.forEach { m ->
                sb.append("%7C${m.lat},${m.lng}")
            }
        }

        sb.append("&key=$apiKey")
        return sb.toString()
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun <T : java.io.Serializable> getSerializableCompat(
        bundle: Bundle,
        key: String,
        clazz: Class<T>
    ): T? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            bundle.getSerializable(key, clazz)
        } else {
            @Suppress("UNCHECKED_CAST")
            bundle.getSerializable(key) as? T
        }
    }


}