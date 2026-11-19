package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FarmerProfile
import com.example.data.repository.MarketPricesResult
import com.example.ui.components.StatusPill
import com.example.ui.theme.FreshEmerald
import com.example.ui.theme.HarvestGoldContainer
import com.example.ui.viewmodel.MarketUiState
import com.example.util.IndianAgriData

@Composable
fun MandiMarketScreen(
  farmer: FarmerProfile,
  marketState: MarketUiState,
  onFetchPrices: (state: String?, district: String?, commodity: String?) -> Unit,
) {
  val context = LocalContext.current
  var commodityQuery by remember { mutableStateOf("") }
  var stateQuery by remember { mutableStateOf(farmer.state) }
  var districtQuery by remember { mutableStateOf(farmer.district) }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 820.dp)
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text("Live Mandi Market Prices", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "Powered by official Agmarknet / data.gov.in agricultural market feed (₹ / Quintal).",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
          Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = commodityQuery,
              onValueChange = { commodityQuery = it },
              label = { Text("Crop / Commodity (e.g., Soybean, Wheat, Onion)") },
              leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("market_commodity_input"),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              OutlinedTextField(
                value = stateQuery,
                onValueChange = { stateQuery = it },
                label = { Text("State") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("market_state_input"),
              )
              OutlinedTextField(
                value = districtQuery,
                onValueChange = { districtQuery = it },
                label = { Text("District") },
                singleLine = true,
                modifier = Modifier.weight(1f).testTag("market_district_input"),
              )
            }
            Button(
              onClick = { onFetchPrices(stateQuery, districtQuery, commodityQuery) },
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("market_search_button"),
              shape = RoundedCornerShape(14.dp),
            ) {
              Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Fetch Live Mandi Rates")
            }
          }
        }
      }

      when (marketState) {
        is MarketUiState.Idle -> {
          item {
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
              shape = RoundedCornerShape(16.dp),
            ) {
              Text(
                text = "Tap 'Fetch Live Mandi Rates' above to query official Agmarknet data. Live prices are never fabricated offline.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
              )
            }
          }
        }
        is MarketUiState.Loading -> {
          item {
            Row(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(24.dp))
              Spacer(Modifier.width(12.dp))
              Text("Querying data.gov.in Agmarknet API...")
            }
          }
        }
        is MarketUiState.Result -> {
          when (val result = marketState.data) {
            is MarketPricesResult.Unavailable -> {
              item {
                Card(
                  colors = CardDefaults.cardColors(containerColor = HarvestGoldContainer),
                  shape = RoundedCornerShape(18.dp),
                ) {
                  Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill(text = "LIVE DATA UNAVAILABLE")
                    Text(
                      text = "Latest live mandi prices could not be retrieved.",
                      style = MaterialTheme.typography.titleMedium,
                    )
                    Text(result.reason, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      OutlinedButton(
                        onClick = {
                          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://agmarknet.gov.in/")))
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                      ) {
                        Text("Agmarknet Portal")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                      }
                      OutlinedButton(
                        onClick = {
                          context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.enam.gov.in/")))
                        },
                        modifier = Modifier.minimumInteractiveComponentSize(),
                      ) {
                        Text("e-NAM Portal")
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
            }
            is MarketPricesResult.Success -> {
              item {
                StatusPill(text = result.source)
              }
              items(result.records) { rec ->
                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                ) {
                  Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Text(
                        text = "${rec.commodity} (${rec.variety})",
                        style = MaterialTheme.typography.titleMedium,
                      )
                      Text(
                        text = "₹${rec.modalPrice}/qtl",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = FreshEmerald,
                      )
                    }
                    Text(
                      text = "${rec.market}, ${rec.district}, ${rec.state} • Arrival: ${rec.arrivalDate}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                      Text("Min: ₹${rec.minPrice}", style = MaterialTheme.typography.labelMedium)
                      Text("Modal: ₹${rec.modalPrice}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                      Text("Max: ₹${rec.maxPrice}", style = MaterialTheme.typography.labelMedium)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun KnowledgeCenterScreen() {
  val context = LocalContext.current
  var query by remember { mutableStateOf("") }
  val categories = remember { listOf("All") + IndianAgriData.knowledgeArticles.map { it.category }.distinct() }
  var selectedCategory by remember { mutableStateOf("All") }
  var expandedId by remember { mutableStateOf<String?>(null) }

  val filtered = remember(query, selectedCategory) {
    IndianAgriData.knowledgeArticles.filter { article ->
      val catMatch = selectedCategory == "All" || article.category == selectedCategory
      val qMatch = query.isBlank() ||
        article.title.contains(query, ignoreCase = true) ||
        article.summary.contains(query, ignoreCase = true) ||
        article.content.contains(query, ignoreCase = true)
      catMatch && qMatch
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 820.dp)
        .fillMaxSize()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        Text("Farming Knowledge Center", style = MaterialTheme.typography.headlineSmall)
        Text(
          text = "Cultivation guides, soil & IPM practices, seasonal calendar, and official Indian government schemes.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      item {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          label = { Text("Search guides, fertilizers, PM-KISAN...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("knowledge_search_input"),
        )
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              label = { Text(cat) },
            )
          }
        }
      }

      items(filtered, key = { it.id }) { article ->
        val isExpanded = expandedId == article.id
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
          modifier = Modifier.fillMaxWidth().clickable {
            expandedId = if (isExpanded) null else article.id
          },
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusPill(text = article.category)
                if (article.isGovtScheme) {
                  Icon(
                    Icons.Default.Verified,
                    contentDescription = "Official government scheme",
                    tint = FreshEmerald,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
              Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse guide" else "Expand guide",
              )
            }
            Text(article.title, style = MaterialTheme.typography.titleMedium)
            Text(
              article.summary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(visible = isExpanded) {
              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(article.content, style = MaterialTheme.typography.bodyMedium)
                if (!article.officialUrl.isNullOrBlank()) {
                  OutlinedButton(
                    onClick = {
                      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.officialUrl)))
                    },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                  ) {
                    Text("Visit Official Govt Portal")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
