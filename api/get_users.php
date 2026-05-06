<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$current_user_id = isset($_GET['user_id']) ? $_GET['user_id'] : 0;

try {
    // First get users that current user follows (accepted)
    $followed_query = "
    SELECT
    u.user_id,
    u.username,
    u.profile_pic,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'accepted') as followers_count,
    'accepted' as follow_status
    FROM users u
    INNER JOIN follows f ON u.user_id = f.following_id
    WHERE f.follower_id = ? AND f.status = 'accepted'
    ORDER BY u.username";

    $stmt = $conn->prepare($followed_query);
    $stmt->bind_param("i", $current_user_id);
    $stmt->execute();
    $followed_result = $stmt->get_result();

    // Get users with pending requests
    $pending_query = "
    SELECT
    u.user_id,
    u.username,
    u.profile_pic,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'accepted') as followers_count,
    'pending' as follow_status
    FROM users u
    INNER JOIN follows f ON u.user_id = f.following_id
    WHERE f.follower_id = ? AND f.status = 'pending'
    ORDER BY u.username";

    $stmt = $conn->prepare($pending_query);
    $stmt->bind_param("i", $current_user_id);
    $stmt->execute();
    $pending_result = $stmt->get_result();

    // Then get other users
    $others_query = "
    SELECT
    u.user_id,
    u.username,
    u.profile_pic,
    (SELECT COUNT(*) FROM follows WHERE following_id = u.user_id AND status = 'accepted') as followers_count,
    'none' as follow_status
    FROM users u
    WHERE u.user_id != ?
    AND u.user_id NOT IN (
        SELECT following_id
        FROM follows
        WHERE follower_id = ?
    )
    ORDER BY RAND()
    LIMIT 20";

    $stmt = $conn->prepare($others_query);
    $stmt->bind_param("ii", $current_user_id, $current_user_id);
    $stmt->execute();
    $others_result = $stmt->get_result();

    // Combine results
    $users = array();

    // Add followed users first
    while ($row = $followed_result->fetch_assoc()) {
        $users[] = array(
            'user_id' => $row['user_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'followers_count' => intval($row['followers_count']),
            'is_following' => true,
            'follow_status' => $row['follow_status']
        );
    }

    // Add pending request users
    while ($row = $pending_result->fetch_assoc()) {
        $users[] = array(
            'user_id' => $row['user_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'followers_count' => intval($row['followers_count']),
            'is_following' => false,
            'follow_status' => $row['follow_status']
        );
    }

    // Add other users
    while ($row = $others_result->fetch_assoc()) {
        $users[] = array(
            'user_id' => $row['user_id'],
            'username' => $row['username'],
            'profile_pic' => $row['profile_pic'],
            'followers_count' => intval($row['followers_count']),
            'is_following' => false,
            'follow_status' => $row['follow_status']
        );
    }

    $response['success'] = true;
    $response['users'] = $users;

} catch (Exception $e) {
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
