<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $sender_id = isset($_POST['sender_id']) ? (int)$_POST['sender_id'] : 0;
    $receiver_id = isset($_POST['receiver_id']) ? (int)$_POST['receiver_id'] : 0;
    $content = isset($_POST['content']) ? trim($_POST['content']) : '';

    if ($sender_id == 0 || $receiver_id == 0 || empty($content)) {
        $response['success'] = false;
        $response['message'] = "Invalid parameters";
        echo json_encode($response);
        exit();
    }

    try {
        // Check mutual follow
        $mutual_query = "
        SELECT 
            (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as user_follows_other,
            (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as other_follows_user
        ";
        $mutual_stmt = $conn->prepare($mutual_query);
        $mutual_stmt->bind_param("iiii", $sender_id, $receiver_id, $receiver_id, $sender_id);
        $mutual_stmt->execute();
        $mutual_result = $mutual_stmt->get_result()->fetch_assoc();

        if ($mutual_result['user_follows_other'] == 0 || $mutual_result['other_follows_user'] == 0) {
            throw new Exception("Cannot message: mutual follow required");
        }

        // Get or create conversation (ensure user1_id < user2_id for consistency)
        $user1 = min($sender_id, $receiver_id);
        $user2 = max($sender_id, $receiver_id);

        $conv_query = "SELECT conversation_id FROM conversations WHERE user1_id = ? AND user2_id = ?";
        $conv_stmt = $conn->prepare($conv_query);
        $conv_stmt->bind_param("ii", $user1, $user2);
        $conv_stmt->execute();
        $conv_result = $conv_stmt->get_result();

        if ($conv_result->num_rows > 0) {
            $conversation_id = $conv_result->fetch_assoc()['conversation_id'];
        } else {
            // Create new conversation
            $create_conv = $conn->prepare("INSERT INTO conversations (user1_id, user2_id) VALUES (?, ?)");
            $create_conv->bind_param("ii", $user1, $user2);
            $create_conv->execute();
            $conversation_id = $conn->insert_id;
        }

        // Insert message
        $msg_stmt = $conn->prepare("INSERT INTO messages (conversation_id, sender_id, content) VALUES (?, ?, ?)");
        $msg_stmt->bind_param("iis", $conversation_id, $sender_id, $content);
        
        if ($msg_stmt->execute()) {
            $message_id = $conn->insert_id;

            // Update conversation timestamp
            $update_conv = $conn->prepare("UPDATE conversations SET updated_at = CURRENT_TIMESTAMP WHERE conversation_id = ?");
            $update_conv->bind_param("i", $conversation_id);
            $update_conv->execute();

            // Get the inserted message details
            $get_msg = $conn->prepare("
                SELECT m.*, u.username, u.profile_pic 
                FROM messages m 
                JOIN users u ON m.sender_id = u.user_id 
                WHERE m.message_id = ?
            ");
            $get_msg->bind_param("i", $message_id);
            $get_msg->execute();
            $msg_data = $get_msg->get_result()->fetch_assoc();

            $response['success'] = true;
            $response['message'] = "Message sent";
            $response['conversation_id'] = $conversation_id;
            $response['message_data'] = array(
                'message_id' => (int)$msg_data['message_id'],
                'sender_id' => (int)$msg_data['sender_id'],
                'sender_username' => $msg_data['username'],
                'sender_profile_pic' => $msg_data['profile_pic'] ?? '',
                'content' => $msg_data['content'],
                'is_read' => false,
                'created_at' => $msg_data['created_at']
            );
        } else {
            throw new Exception("Failed to send message");
        }

    } catch (Exception $e) {
        $response['success'] = false;
        $response['message'] = $e->getMessage();
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
