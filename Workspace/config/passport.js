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
var connection = mysql.createPool({
    host: 'us-cdbr-iron-east-05.cleardb.net',//'localhost',//'ec2-52-53-249-123.us-west-1.compute.amazonaws.com',
    //port: '8900',//'3306',
    user: 'bc4ae62ddac889',//'root',
    password: '880a6aad',//'amey',
    database: 'heroku_8c76196aca775a9'//'cmpe272'
  });

module.exports = function(passport) {

    console.log('passport');
    // used to serialize the user for the session
    passport.serializeUser(function(user, done) {
        console.log('serialize %j', user);
        done(null, user);
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
                        var datasend = {};
                        datasend["twitter_id"]=profile.id;
                        datasend["token"]=token;
                        datasend["username"]=profile.username;
                        datasend["displayName"]=profile.displayName;
                        return done(null, datasend);
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
