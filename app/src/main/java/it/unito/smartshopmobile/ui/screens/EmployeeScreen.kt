/**
 * EmployeeScreen.kt
 *
 * RUOLO MVVM: View Layer (UI - Jetpack Compose)
 * - Schermata dedicata al ruolo Employee (dipendente)
 * - Mappa interattiva 2D del supermercato con SVG
 * - Puramente presentazionale: stato dal EmployeeViewModel
 */
package it.unito.smartshopmobile.ui.screens

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import it.unito.smartshopmobile.viewModel.EmployeeViewModel
import it.unito.smartshopmobile.viewModel.AisleProduct
import it.unito.smartshopmobile.viewModel.StoreAisle

@Composable
fun EmployeeScreen(
    modifier: Modifier = Modifier,
    viewModel: EmployeeViewModel = EmployeeViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mappa Supermercato",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Tocca una corsia sulla mappa per vedere i prodotti disponibili.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        // WebView con SVG interattivo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)

                        // JavaScript Interface per comunicazione SVG -> Kotlin
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onAisleClick(id: String) {
                                Handler(Looper.getMainLooper()).post {
                                    viewModel.selectAisle(id)
                                    Toast.makeText(ctx, "Corsia $id selezionata", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }, "AndroidBridge")

                        // Carica SVG inline (embedded)
                        val svgContent = """
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1200 900">
                              <defs>
                                <style>
                                  .aisle { cursor: pointer; transition: all 0.3s; }
                                  .aisle:hover { filter: brightness(1.1); }
                                  .aisle-selected { stroke: #1976D2; stroke-width: 4; }
                                  .aisle-text { font-family: Arial; font-size: 16px; font-weight: bold; fill: #333; pointer-events: none; }
                                  .aisle-desc { font-family: Arial; font-size: 12px; fill: #666; pointer-events: none; }
                                </style>
                              </defs>
                              <rect width="1200" height="900" fill="#f5f5f5"/>
                              <text x="600" y="40" text-anchor="middle" font-size="28" font-weight="bold" fill="#333">Mappa Supermercato</text>
                              <rect x="500" y="70" width="200" height="40" fill="#4CAF50" stroke="#2E7D32" stroke-width="2" rx="5"/>
                              <text x="600" y="95" text-anchor="middle" font-size="16" font-weight="bold" fill="white">INGRESSO</text>
                              
                              <g id="A1" class="aisle">
                                <rect x="100" y="150" width="300" height="120" fill="#81C784" stroke="#66BB6A" stroke-width="2" rx="8"/>
                                <text x="250" y="195" text-anchor="middle" class="aisle-text">A1</text>
                                <text x="250" y="215" text-anchor="middle" class="aisle-desc">Frutta e Verdura</text>
                              </g>
                              <g id="A2" class="aisle">
                                <rect x="450" y="150" width="300" height="120" fill="#FFB74D" stroke="#FFA726" stroke-width="2" rx="8"/>
                                <text x="600" y="195" text-anchor="middle" class="aisle-text">A2</text>
                                <text x="600" y="215" text-anchor="middle" class="aisle-desc">Panetteria</text>
                              </g>
                              <g id="A3" class="aisle">
                                <rect x="800" y="150" width="300" height="120" fill="#E57373" stroke="#EF5350" stroke-width="2" rx="8"/>
                                <text x="950" y="195" text-anchor="middle" class="aisle-text">A3</text>
                                <text x="950" y="215" text-anchor="middle" class="aisle-desc">Salumeria</text>
                              </g>
                              
                              <g id="B1" class="aisle">
                                <rect x="100" y="310" width="300" height="120" fill="#FFD54F" stroke="#FFCA28" stroke-width="2" rx="8"/>
                                <text x="250" y="355" text-anchor="middle" class="aisle-text">B1</text>
                                <text x="250" y="375" text-anchor="middle" class="aisle-desc">Pasta e Riso</text>
                              </g>
                              <g id="B2" class="aisle">
                                <rect x="450" y="310" width="300" height="120" fill="#A1887F" stroke="#8D6E63" stroke-width="2" rx="8"/>
                                <text x="600" y="355" text-anchor="middle" class="aisle-text">B2</text>
                                <text x="600" y="375" text-anchor="middle" class="aisle-desc">Conserve</text>
                              </g>
                              <g id="B3" class="aisle">
                                <rect x="800" y="310" width="300" height="120" fill="#DCE775" stroke="#D4E157" stroke-width="2" rx="8"/>
                                <text x="950" y="355" text-anchor="middle" class="aisle-text">B3</text>
                                <text x="950" y="375" text-anchor="middle" class="aisle-desc">Condimenti</text>
                              </g>
                              
                              <g id="C1" class="aisle">
                                <rect x="100" y="470" width="300" height="120" fill="#64B5F6" stroke="#42A5F5" stroke-width="2" rx="8"/>
                                <text x="250" y="515" text-anchor="middle" class="aisle-text">C1</text>
                                <text x="250" y="535" text-anchor="middle" class="aisle-desc">Bevande</text>
                              </g>
                              <g id="C2" class="aisle">
                                <rect x="450" y="470" width="300" height="120" fill="#F06292" stroke="#EC407A" stroke-width="2" rx="8"/>
                                <text x="600" y="515" text-anchor="middle" class="aisle-text">C2</text>
                                <text x="600" y="535" text-anchor="middle" class="aisle-desc">Snack e Dolci</text>
                              </g>
                              <g id="C3" class="aisle">
                                <rect x="800" y="470" width="300" height="120" fill="#90CAF9" stroke="#64B5F6" stroke-width="2" rx="8"/>
                                <text x="950" y="515" text-anchor="middle" class="aisle-text">C3</text>
                                <text x="950" y="535" text-anchor="middle" class="aisle-desc">Surgelati</text>
                              </g>
                              
                              <g id="D1" class="aisle">
                                <rect x="100" y="630" width="300" height="120" fill="#BA68C8" stroke="#AB47BC" stroke-width="2" rx="8"/>
                                <text x="250" y="675" text-anchor="middle" class="aisle-text">D1</text>
                                <text x="250" y="695" text-anchor="middle" class="aisle-desc">Detersivi</text>
                              </g>
                              <g id="D2" class="aisle">
                                <rect x="450" y="630" width="300" height="120" fill="#4DB6AC" stroke="#26A69A" stroke-width="2" rx="8"/>
                                <text x="600" y="675" text-anchor="middle" class="aisle-text">D2</text>
                                <text x="600" y="695" text-anchor="middle" class="aisle-desc">Igiene</text>
                              </g>
                              <g id="D3" class="aisle">
                                <rect x="800" y="630" width="300" height="120" fill="#FF8A65" stroke="#FF7043" stroke-width="2" rx="8"/>
                                <text x="950" y="675" text-anchor="middle" class="aisle-text">D3</text>
                                <text x="950" y="695" text-anchor="middle" class="aisle-desc">Pet Care</text>
                              </g>
                              
                              <rect x="400" y="800" width="400" height="60" fill="#757575" stroke="#616161" stroke-width="2" rx="5"/>
                              <text x="600" y="835" text-anchor="middle" font-size="18" font-weight="bold" fill="white">CASSE</text>
                              
                              <script type="text/javascript"><![CDATA[
                                let selectedAisle = null;
                                document.querySelectorAll('.aisle').forEach(el => {
                                  el.addEventListener('click', function() {
                                    const aisleId = this.id;
                                    if (selectedAisle) {
                                      selectedAisle.querySelector('rect').classList.remove('aisle-selected');
                                    }
                                    this.querySelector('rect').classList.add('aisle-selected');
                                    selectedAisle = this;
                                    if (window.AndroidBridge) {
                                      window.AndroidBridge.onAisleClick(aisleId);
                                    }
                                  });
                                });
                              ]]></script>
                            </svg>
                        """.trimIndent()

                        loadDataWithBaseURL(null, svgContent, "image/svg+xml", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Pannello dettagli corsia selezionata
        SelectedAislePanel(selected = uiState.selectedAisle)
    }
}

@Composable
private fun SelectedAislePanel(selected: StoreAisle?) {
    if (selected == null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nessuna corsia selezionata", fontWeight = FontWeight.SemiBold)
                Text(
                    "Tocca una corsia sulla mappa per visualizzare i prodotti disponibili.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Corsia ${selected.id} · ${selected.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = selected.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()
            Text("Prodotti disponibili (${selected.products.size})", style = MaterialTheme.typography.labelLarge)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 8.dp)
            ) {
                items(selected.products) { product ->
                    ProductChip(product)
                }
            }
        }
    }
}

@Composable
private fun ProductChip(product: AisleProduct) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                product.name,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${"%.2f".format(product.price)} €",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (product.tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    product.tags.take(2).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

