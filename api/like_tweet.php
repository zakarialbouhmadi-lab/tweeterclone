<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    // Debug received data
    error_log("Received POST data for like: " . print_r($_POST, true));

    $tweet_id = $_POST['tweet_id'];
    $user_id = $_POST['user_id'];

    // Check if user has already liked the tweet
    $check_stmt = $conn->prepare("SELECT * FROM likes WHERE tweet_id = ? AND user_id = ?");
    $check_stmt->bind_param("ii", $tweet_id, $user_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if ($result->num_rows > 0) {
        // User has already liked, so unlike
        $stmt = $conn->prepare("DELETE FROM likes WHERE tweet_id = ? AND user_id = ?");
        $stmt->bind_param("ii", $tweet_id, $user_id);
        $action = "unliked";
    } else {
        // User hasn't liked, so add like
        $stmt = $conn->prepare("INSERT INTO likes (tweet_id, user_id) VALUES (?, ?)");
        $stmt->bind_param("ii", $tweet_id, $user_id);
        $action = "liked";
    }

    if ($stmt->execute()) {
        // Get updated like count
        $count_stmt = $conn->prepare("SELECT COUNT(*) as count FROM likes WHERE tweet_id = ?");
        $count_stmt->bind_param("i", $tweet_id);
        $count_stmt->execute();
        $count_result = $count_stmt->get_result();
        $count_row = $count_result->fetch_assoc();

        $response['success'] = true;
        $response['action'] = $action;
        $response['likes_count'] = $count_row['count'];
        $response['message'] = "Tweet " . $action;
    } else {
        $response['success'] = false;
        $response['message'] = "Error processing like";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
