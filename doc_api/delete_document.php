<?php
// delete_document.php
// Удаление документа с безопасной записью лога (document_id ставится NULL, чтобы не нарушать FK).
header('Content-Type: application/json; charset=utf-8');
require 'db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'error' => 'Only POST']);
    exit;
}

$id = isset($_POST['id']) ? intval($_POST['id']) : 0;
if ($id <= 0) {
    echo json_encode(['success' => false, 'error' => 'Missing id']);
    exit;
}

mysqli_begin_transaction($conn);

try {
    // 1) Удаляем связанные записи из doc_executor (если таблица существует)
    $tbl = 'doc_executor';
    $check = mysqli_query($conn, "SHOW TABLES LIKE '" . mysqli_real_escape_string($conn, $tbl) . "'");
    if ($check && mysqli_num_rows($check) > 0) {
        $stmtDelExec = mysqli_prepare($conn, "DELETE FROM `doc_executor` WHERE `document_id` = ?");
        if ($stmtDelExec === false) {
            $err = mysqli_error($conn);
            mysqli_rollback($conn);
            echo json_encode(['success' => false, 'error' => 'DB prepare error (doc_executor): ' . $err]);
            exit;
        }
        mysqli_stmt_bind_param($stmtDelExec, 'i', $id);
        if (!mysqli_stmt_execute($stmtDelExec)) {
            $err = mysqli_stmt_error($stmtDelExec);
            mysqli_stmt_close($stmtDelExec);
            mysqli_rollback($conn);
            echo json_encode(['success' => false, 'error' => 'DB execute error (doc_executor): ' . $err]);
            exit;
        }
        mysqli_stmt_close($stmtDelExec);
    }

    // 2) Удаляем связанные записи из log_record (старые записи, связанные с документом)
    $tbl2 = 'log_record';
    $check2 = mysqli_query($conn, "SHOW TABLES LIKE '" . mysqli_real_escape_string($conn, $tbl2) . "'");
    if ($check2 && mysqli_num_rows($check2) > 0) {
        $stmtDelLog = mysqli_prepare($conn, "DELETE FROM `log_record` WHERE `document_id` = ?");
        if ($stmtDelLog === false) {
            $err = mysqli_error($conn);
            mysqli_rollback($conn);
            echo json_encode(['success' => false, 'error' => 'DB prepare error (log_record delete): ' . $err]);
            exit;
        }
        mysqli_stmt_bind_param($stmtDelLog, 'i', $id);
        if (!mysqli_stmt_execute($stmtDelLog)) {
            $err = mysqli_stmt_error($stmtDelLog);
            mysqli_stmt_close($stmtDelLog);
            mysqli_rollback($conn);
            echo json_encode(['success' => false, 'error' => 'DB execute error (log_record delete): ' . $err]);
            exit;
        }
        mysqli_stmt_close($stmtDelLog);
    }

    // 3) Удаляем сам документ
    $stmt = mysqli_prepare($conn, "DELETE FROM `document` WHERE `id` = ? LIMIT 1");
    if ($stmt === false) {
        $err = mysqli_error($conn);
        mysqli_rollback($conn);
        echo json_encode(['success' => false, 'error' => 'DB prepare error (document): ' . $err]);
        exit;
    }
    mysqli_stmt_bind_param($stmt, 'i', $id);
    if (!mysqli_stmt_execute($stmt)) {
        $err = mysqli_stmt_error($stmt);
        mysqli_stmt_close($stmt);
        mysqli_rollback($conn);
        echo json_encode(['success' => false, 'error' => 'DB execute error (document): ' . $err]);
        exit;
    }
    $affected = mysqli_stmt_affected_rows($stmt);
    mysqli_stmt_close($stmt);

    // 4) Безопасное логирование: вставляем запись в log_record с document_id = NULL,
    //    а в текст action кладём информацию о удалённом документе.
    if ($check2 && mysqli_num_rows($check2) > 0) {
        $actionText = "Документ (id={$id}) удалён";
        // предполагаем, что log_record имеет поля: id, document_id (может быть NULL), user_id (опционально), action, created_at ...
        // Вставляем document_id = NULL, чтобы не нарушать FK (если FK существует)
        $ins = mysqli_prepare($conn, "INSERT INTO `log_record` (`document_id`, `action`) VALUES (NULL, ?)");
        if ($ins) {
            mysqli_stmt_bind_param($ins, 's', $actionText);
            @mysqli_stmt_execute($ins);
            @mysqli_stmt_close($ins);
        } else {
            // Если не удалось подготовить вставку лога — не критично, продолжаем
            // (мы уже удалили документ)
        }
    }

    mysqli_commit($conn);

    echo json_encode(['success' => true, 'affected' => intval($affected)]);
    exit;

} catch (Exception $ex) {
    mysqli_rollback($conn);
    echo json_encode(['success' => false, 'error' => 'Exception: ' . $ex->getMessage()]);
    exit;
}
?>
