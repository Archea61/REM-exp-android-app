<?php
require 'db.php';

$sql = "SELECT d.id, d.reg_number, DATE_FORMAT(d.reg_date,'%Y-%m-%d') AS reg_date,
               dt.name AS type_name,
               d.title, d.status, DATE_FORMAT(d.due_date,'%Y-%m-%d') AS due_date,
               e.full_name AS author_name
        FROM `document` d
        LEFT JOIN doc_type dt ON d.type_id = dt.id
        LEFT JOIN employee e ON d.author_id = e.id
        ORDER BY d.id DESC
        LIMIT 1000";

$res = mysqli_query($conn, $sql);
$out = fetch_all_assoc($res);
echo json_encode($out);
?>
