
// var express = require('express');
// var app = express();

module.exports = function(app, passport) {
	var path = require('path');
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
			user : req.user // get the user out of session and pass to template
		});
	});
	

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
