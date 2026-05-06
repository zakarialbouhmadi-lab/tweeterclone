<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$other_user_id = isset($_GET['other_user_id']) ? (int)$_GET['other_user_id'] : 0;

try {
    if ($user_id == 0 || $other_user_id == 0) {
        throw new Exception("Invalid user IDs");
    }

    // Check mutual follow
    $mutual_query = "
    SELECT 
        (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as user_follows_other,
        (SELECT COUNT(*) FROM follows WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as other_follows_user
    ";
    $mutual_stmt = $conn->prepare($mutual_query);
    $mutual_stmt->bind_param("iiii", $user_id, $other_user_id, $other_user_id, $user_id);
    $mutual_stmt->execute();
    $mutual_result = $mutual_stmt->get_result()->fetch_assoc();

    if ($mutual_result['user_follows_other'] == 0 || $mutual_result['other_follows_user'] == 0) {
        $response['success'] = false;
        $response['can_message'] = false;
        $response['message'] = "Cannot message: mutual follow required";
        echo json_encode($response);
        exit();
    }

    // Get or create conversation
    $user1 = min($user_id, $other_user_id);
    $user2 = max($user_id, $other_user_id);

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

    // Get other user info
    $user_query = "SELECT user_id, username, profile_pic FROM users WHERE user_id = ?";
    $user_stmt = $conn->prepare($user_query);
    $user_stmt->bind_param("i", $other_user_id);
    $user_stmt->execute();
    $user_data = $user_stmt->get_result()->fetch_assoc();

    $response['success'] = true;
    $response['can_message'] = true;
    $response['conversation_id'] = $conversation_id;
    $response['other_user'] = array(
        'user_id' => (int)$user_data['user_id'],
        'username' => $user_data['username'],
        'profile_pic' => $user_data['profile_pic'] ?? ''
    );

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
