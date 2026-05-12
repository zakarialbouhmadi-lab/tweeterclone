<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

try {
    // Get total unread message count across all conversations
    $query = "
    SELECT COUNT(*) as unread_count
    FROM messages m
    JOIN conversations c ON m.conversation_id = c.conversation_id
    WHERE (c.user1_id = ? OR c.user2_id = ?)
    AND m.sender_id != ?
    AND m.is_read = 0
    ";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("iii", $user_id, $user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();

    $response['success'] = true;
    $response['unread_count'] = (int)$row['unread_count'];

} catch (Exception $e) {
    log_error('get_unread_count', "Error - user_id:{$user_id} msg:" . $e->getMessage());
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
