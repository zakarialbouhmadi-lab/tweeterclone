<?php
header('Content-Type: application/json');
require_once 'config.php';

$response = array();
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;
$other_user_id = isset($_GET['other_user_id']) ? (int)$_GET['other_user_id'] : 0;

try {
    // Check if both users follow each other (mutual follow)
    $query = "
    SELECT 
        (SELECT COUNT(*) FROM follows 
         WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as user_follows_other,
        (SELECT COUNT(*) FROM follows 
         WHERE follower_id = ? AND following_id = ? AND status = 'accepted') as other_follows_user
    ";

    $stmt = $conn->prepare($query);
    $stmt->bind_param("iiii", $user_id, $other_user_id, $other_user_id, $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $row = $result->fetch_assoc();

    $is_mutual = ($row['user_follows_other'] > 0 && $row['other_follows_user'] > 0);

    $response['success'] = true;
    $response['is_mutual_follow'] = $is_mutual;
    $response['can_message'] = $is_mutual;

} catch (Exception $e) {
    log_error('check_mutual_follow', "Error - user_id:{$user_id} other_user_id:{$other_user_id} msg:" . $e->getMessage());
    $response['success'] = false;
    $response['message'] = "Error: " . $e->getMessage();
}

echo json_encode($response);
?>
