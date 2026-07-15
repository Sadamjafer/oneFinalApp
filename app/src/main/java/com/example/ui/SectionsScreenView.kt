package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SectionItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconColor: Color,
    val badgeText: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreenView(
    onSectionClick: (String) -> Unit
) {
    val sections = listOf(
        SectionItem(
            id = "INCOME_SCREEN",
            title = "واردات الدخل",
            description = "تسجيل ومتابعة الأرباح والتدفقات المالية الواردة للمحل",
            icon = Icons.Filled.TrendingUp,
            iconColor = Color(0xFF2E7D32) // Soft Green
        ),
        SectionItem(
            id = "EXPENSE_SCREEN",
            title = "مصروفات المنصرف",
            description = "تسجيل وتصنيف المصاريف اليومية وتتبع وجهات الصرف",
            icon = Icons.Filled.TrendingDown,
            iconColor = Color(0xFFC62828) // Soft Red
        ),
        SectionItem(
            id = "CLIENTS_SCREEN",
            title = "دفتر العملاء والديون",
            description = "إدارة حسابات الزبائن والبيع الآجل والمدفوعات والمتبقي",
            icon = Icons.Filled.People,
            iconColor = Color(0xFF1565C0) // Soft Blue
        ),
        SectionItem(
            id = "PROFIT_SCREEN",
            title = "الأرباح والخصومات",
            description = "حساب حركة صافي الأرباح التلقائية بعد تصفية الخصومات اليومية",
            icon = Icons.Filled.AttachMoney,
            iconColor = Color(0xFFEF6C00) // Soft Gold/Orange
        ),
        SectionItem(
            id = "REPORTS_SCREEN",
            title = "التقارير والإحصائيات",
            description = "استخراج تقارير مالية تفصيلية يومية وشهرية وفترات مخصصة",
            icon = Icons.Filled.PieChart,
            iconColor = Color(0xFF6A1B9A) // Soft Purple
        ),
        SectionItem(
            id = "SETTINGS",
            title = "الإعدادات والنظام",
            description = "إدارة المحلات، المظهر، كلمة المرور والنسخ الاحتياطي للأخطاء",
            icon = Icons.Filled.Settings,
            iconColor = Color(0xFF455A64) // Soft Blue-Gray
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upper Decorative Banner Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End // RTL layout support
            ) {
                Text(
                    text = "أقسام دفتر الحسابات",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Right
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "اختر القسم المطلوب لإدارة وتتبع العمليات المالية الخاصة بك بكل سهولة ودقة.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Right,
                    lineHeight = 18.sp
                )
            }
        }

        // List of Section Cards (Adaptive Grid for landscape support)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 320.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sections) { section ->
                SectionCard(
                    section = section,
                    onClick = { onSectionClick(section.id) }
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
            }
        }
    }
}

@Composable
fun SectionCard(
    section: SectionItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End // Arabic RTL
        ) {
            // Text Content (Takes remaining space, aligned to right)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (section.badgeText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = section.badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        text = section.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Right
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = section.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Right,
                    lineHeight = 16.sp
                )
            }

            // Circle Icon Container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = section.iconColor.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = section.title,
                    tint = section.iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
