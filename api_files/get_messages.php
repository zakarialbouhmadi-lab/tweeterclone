<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$conversation_id = isset($_GET['conversation_id']) ? (int)$_GET['conversation_id'] : 0;
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$limit = isset($_GET['limit']) ? (int)$_GET['limit'] : 50;
$before_id = isset($_GET['before_id']) ? (int)$_GET['before_id'] : 0;

try {
    // Verify user is part of this conversation
    $check_query = "SELECT conversation_id FROM conversations WHERE conversation_id = ? AND (user1_id = ? OR user2_id = ?)";
    $check_stmt = $conn->prepare($check_query);
    $check_stmt->bind_param("iii", $conversation_id, $user_id, $user_id);
    $check_stmt->execute();
    
    if ($check_stmt->get_result()->num_rows == 0) {
        throw new Exception("Unauthorized access to conversation");
    }

    // Mark messages as read
    $update_query = "UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND sender_id != ? AND is_read = 0";
    $update_stmt = $conn->prepare($update_query);
    $update_stmt->bind_param("ii", $conversation_id, $user_id);
    $update_stmt->execute();

    // Get messages
    if ($before_id > 0) {
        $query = "
        SELECT 
            m.message_id,
            m.sender_id,
            u.username as sender_username,
            u.profile_pic as sender_profile_pic,
            m.content,
            m.is_read,
            m.created_at
        FROM messages m
        JOIN users u ON m.sender_id = u.user_id
        WHERE m.conversation_id = ? AND m.message_id < ?
        ORDER BY m.created_at DESC
        LIMIT ?
        ";
        $stmt = $conn->prepare($query);
        $stmt->bind_param("iii", $conversation_id, $before_id, $limit);
    } else {
        $query = "
        SELECT 
            m.message_id,
            m.sender_id,
            u.username as sender_username,
            u.profile_pic as sender_profile_pic,
            m.content,
            m.is_read,
            m.created_at
        FROM messages m
        JOIN users u ON m.sender_id = u.user_id
        WHERE m.conversation_id = ?
        ORDER BY m.created_at DESC
        LIMIT ?
        ";
        $stmt = $conn->prepare($query);
        $stmt->bind_param("ii", $conversation_id, $limit);
    }
    
    $stmt->execute();
    $result = $stmt->get_result();

    $messages = array();
    while ($row = $result->fetch_assoc()) {
        $messages[] = array(
            'message_id' => (int)$row['message_id'],
            'sender_id' => (int)$row['sender_id'],
            'sender_username' => $row['sender_username'],
            'sender_profile_pic' => $row['sender_profile_pic'] ?? '',
            'content' => $row['content'],
            'is_read' => (bool)$row['is_read'],
            'created_at' => $row['created_at']
        );
    }

    // Reverse to get chronological order
    $messages = array_reverse($messages);

    $response['success'] = true;
    $response['messages'] = $messages;

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
