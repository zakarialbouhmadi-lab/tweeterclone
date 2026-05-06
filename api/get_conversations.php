<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

try {
    // Get all conversations for this user with last message and unread count
    $query = "
    SELECT 
        c.conversation_id,
        c.updated_at,
        CASE 
            WHEN c.user1_id = ? THEN c.user2_id 
            ELSE c.user1_id 
        END as other_user_id,
        u.username as other_username,
        u.profile_pic as other_profile_pic,
        (SELECT content FROM messages WHERE conversation_id = c.conversation_id ORDER BY created_at DESC LIMIT 1) as last_message,
        (SELECT created_at FROM messages WHERE conversation_id = c.conversation_id ORDER BY created_at DESC LIMIT 1) as last_message_time,
        (SELECT sender_id FROM messages WHERE conversation_id = c.conversation_id ORDER BY created_at DESC LIMIT 1) as last_sender_id,
        (SELECT COUNT(*) FROM messages WHERE conversation_id = c.conversation_id AND sender_id != ? AND is_read = 0) as unread_count
    FROM conversations c
    JOIN users u ON u.user_id = CASE WHEN c.user1_id = ? THEN c.user2_id ELSE c.user1_id END
    WHERE c.user1_id = ? OR c.user2_id = ?
    ORDER BY c.updated_at DESC
    ";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("iiiii", $user_id, $user_id, $user_id, $user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $conversations = array();
    while ($row = $result->fetch_assoc()) {
        // Only include conversations that have messages
        if ($row['last_message'] !== null) {
            $conversations[] = array(
                'conversation_id' => (int)$row['conversation_id'],
                'other_user_id' => (int)$row['other_user_id'],
                'other_username' => $row['other_username'],
                'other_profile_pic' => $row['other_profile_pic'] ?? '',
                'last_message' => $row['last_message'],
                'last_message_time' => $row['last_message_time'],
                'last_sender_id' => (int)$row['last_sender_id'],
                'unread_count' => (int)$row['unread_count']
            );
        }
    }

    $response['success'] = true;
    $response['conversations'] = $conversations;

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
