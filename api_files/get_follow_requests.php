<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

try {
    // Get pending follow requests for this user
    $query = "
    SELECT 
        f.follower_id,
        f.created_at as request_date,
        u.username,
        u.profile_pic,
        u.bio
    FROM follows f
    JOIN users u ON f.follower_id = u.user_id
    WHERE f.following_id = ? AND f.status = 'pending'
    ORDER BY f.created_at DESC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $requests = array();
    while ($row = $result->fetch_assoc()) {
        $requests[] = array(
            'user_id' => (int)$row['follower_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'bio' => $row['bio'] ?? '',
            'request_date' => $row['request_date']
        );
    }

    $response['success'] = true;
    $response['requests'] = $requests;
    $response['count'] = count($requests);

} catch (Exception $e) {
    log_error('get_follow_requests', "Error - user_id:{$user_id} msg:" . $e->getMessage());
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
