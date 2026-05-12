<?php
header('Content-Type: application/json');
require_once 'config.php';

$conversation_id = isset($_GET['conversation_id']) ? (int)$_GET['conversation_id'] : 0;
$user_id         = isset($_GET['user_id'])         ? (int)$_GET['user_id']         : 0;
$limit           = isset($_GET['limit'])           ? (int)$_GET['limit']           : 50;
$before_id       = isset($_GET['before_id'])       ? (int)$_GET['before_id']       : 0;

try {
    // Verify user belongs to this conversation
    $check = $conn->prepare("SELECT conversation_id FROM conversations WHERE conversation_id = ? AND (user1_id = ? OR user2_id = ?)");
    $check->bind_param("iii", $conversation_id, $user_id, $user_id);
    $check->execute();
    if ($check->get_result()->num_rows === 0) {
        log_warn('get_messages', "Unauthorized access - user_id:{$user_id} conversation_id:{$conversation_id}");
        throw new Exception("Unauthorized access to conversation");
    }

    // Mark incoming messages as read
    $upd = $conn->prepare("UPDATE messages SET is_read = 1 WHERE conversation_id = ? AND sender_id != ? AND is_read = 0");
    $upd->bind_param("ii", $conversation_id, $user_id);
    $upd->execute();

    // Fetch messages; choose the right encrypted key based on who is asking
    $key_col = "CASE WHEN m.sender_id = $user_id THEN m.encrypted_key_for_sender ELSE m.encrypted_key_for_receiver END AS encrypted_key";

    if ($before_id > 0) {
        $query = "
            SELECT
                m.message_id, m.sender_id,
                u.username AS sender_username,
                u.profile_pic AS sender_profile_pic,
                m.content, m.is_read, m.created_at,
                m.is_encrypted, $key_col
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
                m.message_id, m.sender_id,
                u.username AS sender_username,
                u.profile_pic AS sender_profile_pic,
                m.content, m.is_read, m.created_at,
                m.is_encrypted, $key_col
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

    $messages = [];
    while ($row = $result->fetch_assoc()) {
        $messages[] = [
            'message_id'         => (int)$row['message_id'],
            'sender_id'          => (int)$row['sender_id'],
            'sender_username'    => $row['sender_username'],
            'sender_profile_pic' => $row['sender_profile_pic'] ?? '',
            'content'            => $row['content'],
            'is_read'            => (bool)$row['is_read'],
            'created_at'         => $row['created_at'],
            'is_encrypted'       => (bool)$row['is_encrypted'],
            'encrypted_key'      => $row['encrypted_key'],
        ];
    }

    // Return in chronological order
    $messages = array_reverse($messages);

    echo json_encode(['success' => true, 'messages' => $messages]);

} catch (Exception $e) {
    log_error('get_messages', "Error - user_id:{$user_id} conversation_id:{$conversation_id} msg:" . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Error: ' . $e->getMessage()]);
}
?>
