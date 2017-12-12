var mysql = require('mysql');
// load all the things we need
var LocalStrategy    = require('passport-local').Strategy;
var FacebookStrategy = require('passport-facebook').Strategy;
var TwitterStrategy  = require('passport-twitter').Strategy;

// load up the user model
// var User       = require('../app/models/user');

// load the auth variables
var configAuth = require('./auth');

//connection
var connection = mysql.createConnection({
    host     : "localhost",
    user     : "root",
    password : "admin",
    database : "twitterlogin"
  });

module.exports = function(passport) {

    console.log('passport');
    // used to serialize the user for the session
    passport.serializeUser(function(user, done) {
        console.log('serialize %j', user.username);
        done(null, "@"+user.username);
    });

    // used to deserialize the user
    passport.deserializeUser(function(id, done) {
            done(null, id);
    });
    
    // code for login (use('local-login', new LocalStategy))
    // code for signup (use('local-signup', new LocalStategy))
    // code for facebook (use('facebook', new FacebookStrategy))

    // =========================================================================
    // TWITTER =================================================================
    // =========================================================================
    passport.use(new TwitterStrategy({

        consumerKey     : configAuth.twitterAuth.consumerKey,
        consumerSecret  : configAuth.twitterAuth.consumerSecret,
        callbackURL     : configAuth.twitterAuth.callbackURL

    },
    function(token, tokenSecret, profile, done) {

        // make the code asynchronous
	 	// User.findOne won't fire until we have all our data back from Twitter
        process.nextTick(function() {

            connection.query("SELECT * from profile where twitter_id="+profile.id,function(err,rows,fields){
                if(err) throw err;
                console.log('row %j', rows);
                if(rows.length===0)
                  {
                    console.log("There is no such user, adding now");
                    connection.query("INSERT into profile(twitter_id, token, username, displayName) VALUES('"+profile.id+"','"+token+"','"+profile.username+"','"+profile.displayName+"')",
                    function(err, result) {
                        rows["twitter_id"]=profile.id;
                        rows["token"]=token;
                        rows["username"]=profile.username;
                        rows["displayName"]=profile.displayName;
                        return done(null, profile);
                    });
                  }
                  else
                    {
                      console.log("User already exists in database");
                      return done(null, rows[0]);
                    }
                  });
        });

    }));

};
