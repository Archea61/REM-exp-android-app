<?php
// get_stats.php - возвращает несмешиваемые категории:
// total, completed, overdue, active (active = in_work_active + drafts)
// а также дополнительные поля для проверки согласованности

require 'db.php';

// удобная helper-функция для безопасного выполнения запроса и извлечения count
function count_query($conn, $sql) {
    $res = mysqli_query($conn, $sql);
    if (!$res) return 0;
    $row = mysqli_fetch_assoc($res);
    return isset($row['c']) ? (int)$row['c'] : 0;
}

// Общее число документов
$total = count_query($conn, "SELECT COUNT(*) AS c FROM `document`");

// Завершённые (утвержден)
$completed = count_query($conn,
    "SELECT COUNT(*) AS c FROM `document` WHERE LOWER(COALESCE(status, '')) = 'утвержден'");

// Просроченные: только те, которые находятся "в работе" и whose due_date < CURDATE()
$overdue = count_query($conn,
    "SELECT COUNT(*) AS c FROM `document` 
     WHERE LOWER(COALESCE(status, '')) = 'в работе' 
       AND due_date IS NOT NULL 
       AND DATE(due_date) < CURDATE()");

// Документы "в работе" и НЕ просроченные (due_date NULL или >= today)
$in_work_active = count_query($conn,
    "SELECT COUNT(*) AS c FROM `document`
     WHERE LOWER(COALESCE(status, '')) = 'в работе'
       AND (due_date IS NULL OR DATE(due_date) >= CURDATE())");

// Черновики
$drafts = count_query($conn,
    "SELECT COUNT(*) AS c FROM `document`
     WHERE LOWER(COALESCE(status, '')) = 'черновик'");

// Активные = in_work_active + drafts
$active = $in_work_active + $drafts;

// Контроль согласованности: сумма частей и total
$sum_parts = $completed + $overdue + $active;
$check_sum = ($sum_parts === $total);

// Итоговый JSON
echo json_encode([
    'total' => $total,
    'completed' => $completed,
    'overdue' => $overdue,
    'in_work_active' => $in_work_active,
    'drafts' => $drafts,
    'active' => $active,
    // для отладки/журнала
    'sum_parts' => $sum_parts,
    'check_sum' => $check_sum
]);
?>
