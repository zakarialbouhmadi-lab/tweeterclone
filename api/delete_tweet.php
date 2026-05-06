<?php
header('Content-Type: application/json');
require_once 'config.php';
$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $tweet_id = $_POST['tweet_id'];
    $user_id = $_POST['user_id'];

    // Verify the tweet belongs to the user
    $check_stmt = $conn->prepare("SELECT user_id FROM tweets WHERE tweet_id = ?");
    $check_stmt->bind_param("i", $tweet_id);
    $check_stmt->execute();
    $result = $check_stmt->get_result();

    if($result->fetch_assoc()['user_id'] != $user_id) {
        $response['success'] = false;
        $response['message'] = "Unauthorized to delete this tweet";
        echo json_encode($response);
        exit();
    }

    // Delete associated likes and comments first
    $stmt = $conn->prepare("DELETE FROM likes WHERE tweet_id = ?");
    $stmt->bind_param("i", $tweet_id);
    $stmt->execute();

    $stmt = $conn->prepare("DELETE FROM comments WHERE tweet_id = ?");
    $stmt->bind_param("i", $tweet_id);
    $stmt->execute();

    // Delete the tweet
    $stmt = $conn->prepare("DELETE FROM tweets WHERE tweet_id = ? AND user_id = ?");
    $stmt->bind_param("ii", $tweet_id, $user_id);

    if ($stmt->execute()) {
        $response['success'] = true;
        $response['message'] = "Tweet deleted successfully";
    } else {
        $response['success'] = false;
        $response['message'] = "Error deleting tweet";
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
}

echo json_encode($response);
?>
