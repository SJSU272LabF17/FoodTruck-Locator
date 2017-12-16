
// var express = require('express');
// var app = express();
var mysql = require('mysql');
var js2xmlparser = require('js2xmlparser');

module.exports = function(app, passport) {
	var path = require('path');

	var mconn = mysql.createPool({
        host: 'us-cdbr-iron-east-05.cleardb.net',//'localhost',//'ec2-52-53-249-123.us-west-1.compute.amazonaws.com',
		//port: '8900',//'3306',
		user: 'bc4ae62ddac889',//'root',
		password: '880a6aad',//'amey',
		database: 'heroku_8c76196aca775a9'//'cmpe272'
	});
	
	//route for home page
	app.get('/', function(req, res) {
		console.log('index'  + res);
		res.render('index.ejs'); // load the index.ejs file
	});

	// route for login form
	// route for processing the login form
	// route for signup form
	// route for processing the signup form

	// route for showing the profile page
	app.get('/profile', isLoggedIn, function(req, res) {
		console.log('profile log %j', req.user);
		console.log('profile res %j',res);
		res.render('home', {
			username : req.user.username, // get the user out of session and pass to template
			displayName : req.user.displayName
		});
	});
	
	app.post('/map', (req, res) => {
		console.log(req.body.lat);
		if(req.body.user !== undefined) {
			var sql = "SELECT event_id, merchantName, address, lat, lon, twitterHandle, eventDate FROM events WHERE twitterHandle = ?";
			var query = [req.body.user];
		}
        else if(req.body.date !== "") {
            var sql = "SELECT event_id, merchantName, address, lat, lon, twitterHandle, eventDate, ( 3959 * acos( cos( radians(?) ) * cos( radians( lat ) ) * cos( radians( lon ) - radians(?) ) + sin( radians(?) ) * sin( radians( lat ) ) ) ) AS distance FROM events WHERE eventDate = ? HAVING distance < ? ORDER BY distance";
            var query = [req.body.lat, req.body.lon, req.body.lat, req.body.date, req.body.radius];
        }
        else {
			console.log('second');
            var sql = "SELECT event_id, merchantName, address, lat, lon, twitterHandle, eventDate, ( 3959 * acos( cos( radians(?) ) * cos( radians( lat ) ) * cos( radians( lon ) - radians(?) ) + sin( radians(?) ) * sin( radians( lat ) ) ) ) AS distance FROM events HAVING distance < ? ORDER BY distance";
			var query = [req.body.lat, req.body.lon, req.body.lat, req.body.radius];
        }
        mconn.query(sql, query, function(err, rows, fields) {
            console.log('inside query');
            if(err) 
                console.log(err);
            else {
				console.log('not err ' + rows);
                var markerArray = [];
                rows.forEach(function(obj) {
                    console.log(obj.eventDate);
                    var marker = {};
                    marker["twitter_text"] = obj.originalTweet;
                    marker["date"] = obj.eventDate.getDate();
                    marker["twitter"] = obj.twitterHandle;
                    marker["distance"] = obj.distance;
                    marker["lat"] = obj.lat;
                    marker["lng"] = obj.lon;
                    marker["address"] = obj.address;
                    marker["name"] = obj.merchantName;
                    marker["id"] = obj.event_id;
                    var markerVal = {};
                    markerVal["@"] = marker;
                    markerArray.push(markerVal);
                });
				console.log(markerArray);
				var markerobj = {};
				markerobj["marker"] = markerArray;
                var xmlobj = js2xmlparser.parse("markers", markerobj);
                console.log(xmlobj);
				res.set('Content-Type', 'text/xml');
				res.send(xmlobj);
            } 
        })
    })
	// facebook routes

	// =====================================
	// TWITTER ROUTES =====================
	// =====================================
	// route for twitter authentication and login
	app.get('/auth/twitter', passport.authenticate('twitter'));

	// handle the callback after twitter has authenticated the user
	app.get('/auth/twitter/callback',
		passport.authenticate('twitter', {
			successRedirect : '/profile',
			failureRedirect : '/'
		}));

};

// route middleware to make sure a user is logged in
function isLoggedIn(req, res, next) {

	console.log('next' + req.profile);
	// if user is authenticated in the session, carry on
	if (req.isAuthenticated())
		return next();

	// if they aren't redirect them to the home page
	res.redirect('/');
}
