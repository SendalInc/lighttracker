import React from 'react';
import ReactDOM from 'react-dom';
import HomeScreen from './HomeScreen.jsx'
import InfoDetails from './InfoDetails.jsx'
import { toast, Slide } from 'react-toastify';
import { Switch, Route, BrowserRouter } from 'react-router-dom'
import styles from './Styles.jsx'
import injectSheet from 'react-jss'
import io from 'socket.io-client';

toast.configure({
    hideProgressBar: true,
    position: 'top-center',
    newestOnTop: true,
    autoClose: 4000,
    transition: Slide
})

var contentNode = document.getElementById('contents');

var webProtocol = location.protocol + "//"

var websocketPathExtension

var globalPathComponents = window.location.pathname.split('/')
if (globalPathComponents.length > 2) {
    websocketPathExtension = globalPathComponents[2]
} else {
    console.log("Bad URL format: " + window.location.pathname)
}

var socket = io(
    {
        path: '/v1/pss/ws',
        reconnection: true,
        reconnectionDelay: 1000,
        randomizationFactor: 0,
        reconnectionDelayMax: 5000,
        reconnectionAttempts: Infinity,
        transports: ['polling']
    })

export { socket, webProtocol }

window.onerror = function (msg, url, lineNo, columnNo, error) {
    // send erorr to the server
    fetch(webProtocol + document.location.hostname + ":" + document.location.port
        + "/v1/pss/5dfe5f55fdc82494c81a339e/jserror",
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                msg,
                url,
                lineNo,
                columnNo,
                stack: error.stack
            })
        })
        .then(response => {
            if (response.ok != true) {
                console.log("Failed to send error report " + response.status)
            }
        })
        .catch(error => {
            console.log("Error in sending error report - " + error)
        })

    return false // false means the default error reporting will still occur
}

export default class App extends React.Component {

    constructor() {
        super();
    }

    render() {
        return (
            <BrowserRouter>
                <Switch>
                    <Route exact path={`/v1/pss/:applicationid/users/:userid/homes/:homeid/assets/index.html`} component={(props) => <HomeScreen classes={this.props.classes} applicationId={props.match.params.applicationid} userId={props.match.params.userid} homeId={props.match.params.homeid} />} />
                    <Route exact path={`/v1/pss/:applicationid/users/:userid/homes/:homeid/routes/infodetails`} component={(props) => <InfoDetails classes={this.props.classes} userId={props.match.params.userid} homeId={props.match.params.homeid} applicationId={props.match.params.applicationid} />} />
                </Switch>
            </BrowserRouter>
        )
    }
}

const StyledApp = injectSheet(styles)(App)

ReactDOM.render(<StyledApp />, contentNode)
