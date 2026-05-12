<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$current_user_id = isset($_GET['current_user_id']) ? (int)$_GET['current_user_id'] : 0;

try {
    // Get users that this user is following (accepted only)
    $query = "
    SELECT 
        u.user_id,
        u.username,
        u.profile_pic,
        u.bio,
        (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'accepted') as followers_count,
        EXISTS(SELECT 1 FROM follows WHERE follower_id = ? AND following_id = u.user_id AND status = 'accepted') as is_following
    FROM follows f
    JOIN users u ON f.following_id = u.user_id
    WHERE f.follower_id = ? AND f.status = 'accepted'
    ORDER BY f.created_at DESC";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("ii", $current_user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();

    $following = array();
    while ($row = $result->fetch_assoc()) {
        $following[] = array(
            'user_id' => (int)$row['user_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'bio' => $row['bio'] ?? '',
            'followers_count' => (int)$row['followers_count'],
            'is_following' => (bool)$row['is_following']
        );
    }

    $response['success'] = true;
    $response['following'] = $following;
    $response['count'] = count($following);

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
