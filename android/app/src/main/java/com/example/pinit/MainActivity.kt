package com.example.pinit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.pinit.ui.theme.PinitTheme
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repo = SearchRepository()

// 1. 제목/내용 검색
        repo.search(
            keyword = "제주"
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "1. 제목/내용 검색: ${it.title}")
            }
        }

// 2. 조건 검색
        repo.search(
            travelerCount = 2L,
            location = "한국",
            selectedMonth = "2026-05"
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "2. 조건 검색: ${it.title}")
            }
        }

// 3. 해시태그 검색
        repo.search(
            selectedHashtags = listOf("맛집")
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "3. 해시태그 검색: ${it.title}")
            }
        }

// 4. 조건 + 제목/내용
        repo.search(
            keyword = "제주",
            travelerCount = 2L,
            location = "제주"
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "4. 조건+제목/내용: ${it.title}")
            }
        }

// 5. 조건 + 해시태그
        repo.search(
            travelerCount = 2L,
            location = "한국",
            selectedHashtags = listOf("맛집")
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "5. 조건+해시태그: ${it.title}")
            }
        }

// 6. 조건 + 해시태그 + 제목/내용
        repo.search(
            keyword = "제주",
            travelerCount = 2L,
            location = "한국",
            selectedMonth = "2026-05",
            selectedHashtags = listOf("힐링", "맛집")
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "6. 조건+해시태그+제목/내용: ${it.title}")
            }
        }

// 7. 해시태그 + 제목/내용
        repo.search(
            keyword = "제주",
            selectedHashtags = listOf("맛집")
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "7. 해시태그+제목/내용: ${it.title}")
            }
        }

// 8. 해시태그 중복 선택
        repo.search(
            selectedHashtags = listOf("힐링", "맛집", "카페")
        ) { posts ->
            posts.forEach {
                Log.d("SearchTest", "8. 해시태그 중복 선택: ${it.title}")
            }
        }

 // 9. 날짜 범위 선택
        repo.search(
            filterStartDate = "2026-05-01",
            filterEndDate = "2026-05-31"
        ) { posts ->
            Log.d("SearchTest", "날짜 범위 검색 개수: ${posts.size}")

            posts.forEach {
                Log.d("SearchTest", "9. 날짜 범위 검색 결과: ${it.title}")
            }
        }

        setContent {
            PinitTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PinitTheme {
        Greeting("Android")
    }
}