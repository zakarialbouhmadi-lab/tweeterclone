<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $content = $_POST['content'];
    $user_id = $_POST['user_id'];
    $image = isset($_POST['image']) ? $_POST['image'] : null;

    $stmt = $conn->prepare("INSERT INTO tweets (user_id, content, image) VALUES (?, ?, ?)");
    $stmt->bind_param("iss", $user_id, $content, $image);

    if ($stmt->execute()) {
        $tweet_id = $conn->insert_id;
        $response['success'] = true;
        $response['message'] = "Tweet created successfully";
        $response['tweet_id'] = $tweet_id;
        log_info('create_tweet', "Tweet created - user_id:{$user_id} tweet_id:{$tweet_id} has_image:" . ($image ? 'yes' : 'no'));
    } else {
        $response['success'] = false;
        $response['message'] = "Error creating tweet";
        log_error('create_tweet', "DB insert failed - user_id:{$user_id}");
    }
} else {
    $response['success'] = false;
    $response['message'] = "Invalid request method";
    log_warn('create_tweet', "Invalid request method: " . $_SERVER['REQUEST_METHOD']);
}

echo json_encode($response);
?>