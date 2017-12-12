<?php
require("dbcon.php");
$center_lat = $_GET["lat"];
$center_lng = $_GET["lng"];
$radius = $_GET["radius"];
$date = $_GET["date"];
$dom = new DOMDocument("1.0");
$dom->formatOutput = true;
$node = $dom->createElement("markers");
$parnode = $dom->appendChild($node);
$connection=mysqli_connect ($servername, $username, $password,$database);
if (!$connection) {
  die("Not connected : " . mysqli_connection_error());
}
if($date!="")
{
    $query = sprintf("SELECT event_id, merchantName, address, lat, lon, twitterHandle, eventDate, ( 3959 * acos( cos( radians('%s') ) * cos( radians( lat ) ) * cos( radians( lon ) - radians('%s') ) + sin( radians('%s') ) * sin( radians( lat ) ) ) ) AS distance FROM markers WHERE date = '%s' HAVING distance < '%s' ORDER BY distance",
      mysqli_real_escape_string($connection,$center_lat),
      mysqli_real_escape_string($connection,$center_lng),
      mysqli_real_escape_string($connection,$center_lat),
      mysqli_real_escape_string($connection,$date),
      mysqli_real_escape_string($connection,$radius));
}
else
{
    $query = sprintf("SELECT event_id, merchantName, address, lat, lon, twitterHandle, eventDate, ( 3959 * acos( cos( radians('%s') ) * cos( radians( lat ) ) * cos( radians( lon ) - radians('%s') ) + sin( radians('%s') ) * sin( radians( lat ) ) ) ) AS distance FROM markers HAVING distance < '%s' ORDER BY distance",
      mysqli_real_escape_string($connection,$center_lat),
      mysqli_real_escape_string($connection,$center_lng),
      mysqli_real_escape_string($connection,$center_lat),
      mysqli_real_escape_string($connection,$radius));
}
$result = mysqli_query($connection,$query);
// $result = mysqli_query($connection,$query);
if (!$result) {
  die("Invalid query: " . mysqli_error($connection));
}
header("Content-type: text/xml");
while ($row = mysqli_fetch_assoc($result)){
  $node = $dom->createElement("marker");
  $newnode = $parnode->appendChild($node);
  $newnode->setAttribute("id", $row['event_id']);
  $newnode->setAttribute("name", $row['merchantName']);
  $newnode->setAttribute("address", $row['address']);
  $newnode->setAttribute("lat", $row['lat']);
  $newnode->setAttribute("lng", $row['lon']);
  $newnode->setAttribute("distance", $row['distance']);
  $newnode->setAttribute("twitter", $row['twitterHandle']);
  $newnode->setAttribute("date", $row['eventDate']);
}

echo $dom->saveXML();
?>