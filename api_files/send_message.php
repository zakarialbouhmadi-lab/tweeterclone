<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(['success' => false, 'message' => 'Invalid request method']);
    exit();
}

$sender_id   = isset($_POST['sender_id'])   ? (int)$_POST['sender_id']   : 0;
$receiver_id = isset($_POST['receiver_id']) ? (int)$_POST['receiver_id'] : 0;
$content     = isset($_POST['content'])     ? trim($_POST['content'])     : '';

$encrypted_key_for_sender   = isset($_POST['encrypted_key_for_sender'])   ? $_POST['encrypted_key_for_sender']   : null;
$encrypted_key_for_receiver = isset($_POST['encrypted_key_for_receiver']) ? $_POST['encrypted_key_for_receiver'] : null;
$is_encrypted = ($encrypted_key_for_sender !== null && $encrypted_key_for_receiver !== null) ? 1 : 0;

if ($sender_id === 0 || $receiver_id === 0 || empty($content)) {
    echo json_encode(['success' => false, 'message' => 'Invalid parameters']);
    exit();
}

try {
    // Check mutual follow
    $mutual_stmt = $conn->prepare("
        SELECT
            (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') AS user_follows_other,
            (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') AS other_follows_user
    ");
    $mutual_stmt->bind_param("iiii", $sender_id, $receiver_id, $receiver_id, $sender_id);
    $mutual_stmt->execute();
    $mutual = $mutual_stmt->get_result()->fetch_assoc();

    if ($mutual['user_follows_other'] == 0 || $mutual['other_follows_user'] == 0) {
        log_warn('send_message', "Blocked - no mutual follow - sender_id:{$sender_id} receiver_id:{$receiver_id}");
        throw new Exception("Cannot message: mutual follow required");
    }

    // Get or create conversation (user1_id always < user2_id)
    $user1 = min($sender_id, $receiver_id);
    $user2 = max($sender_id, $receiver_id);

    $conv_stmt = $conn->prepare("SELECT conversation_id FROM conversations WHERE user1_id = ? AND user2_id = ?");
    $conv_stmt->bind_param("ii", $user1, $user2);
    $conv_stmt->execute();
    $conv_result = $conv_stmt->get_result();

    if ($conv_result->num_rows > 0) {
        $conversation_id = $conv_result->fetch_assoc()['conversation_id'];
    } else {
        $create_conv = $conn->prepare("INSERT INTO conversations (user1_id, user2_id) VALUES (?, ?)");
        $create_conv->bind_param("ii", $user1, $user2);
        $create_conv->execute();
        $conversation_id = $conn->insert_id;
    }

    // Insert message
    $msg_stmt = $conn->prepare("
        INSERT INTO messages
            (conversation_id, sender_id, content, encrypted_key_for_sender, encrypted_key_for_receiver, is_encrypted)
        VALUES (?, ?, ?, ?, ?, ?)
    ");
    $msg_stmt->bind_param("iisssi",
        $conversation_id, $sender_id, $content,
        $encrypted_key_for_sender, $encrypted_key_for_receiver, $is_encrypted);

    if (!$msg_stmt->execute()) {
        throw new Exception("Failed to send message");
    }

    $message_id = $conn->insert_id;

    // Update conversation timestamp
    $upd = $conn->prepare("UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE conversation_id = ?");
    $upd->bind_param("i", $conversation_id);
    $upd->execute();

    // Return inserted message details
    $get_msg = $conn->prepare("
        SELECT m.*, u.username, u.profile_pic
        FROM messages m
        JOIN users u ON m.sender_id = u.user_id
        WHERE m.message_id = ?
    ");
    $get_msg->bind_param("i", $message_id);
    $get_msg->execute();
    $msg = $get_msg->get_result()->fetch_assoc();

    log_info('send_message', "Message sent - sender_id:{$sender_id} receiver_id:{$receiver_id} conversation_id:{$conversation_id} message_id:{$message_id} encrypted:{$is_encrypted}");
    $response['success'] = true;
    $response['message'] = "Message sent";
    $response['conversation_id'] = $conversation_id;
    $response['message_data'] = [
        'message_id'               => (int)$msg['message_id'],
        'sender_id'                => (int)$msg['sender_id'],
        'sender_username'          => $msg['username'],
        'sender_profile_pic'       => $msg['profile_pic'] ?? '',
        'content'                  => $msg['content'],
        'encrypted_key'            => $msg['encrypted_key_for_sender'],
        'is_encrypted'             => (bool)$msg['is_encrypted'],
        'is_read'                  => false,
        'created_at'               => $msg['created_at'],
    ];

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = $e->getMessage();
    log_error('send_message', "Error - sender_id:{$sender_id} receiver_id:{$receiver_id} msg:" . $e->getMessage());
}

echo json_encode($response);
?>
