// CroneEngine_PolyPercIcarus
// pulse wave with perc envelopes, triggered on freq
Engine_PolyPercIcarus : CroneEngine {
	var pg;
    var amp=0.3;
    var release=0.5;
    var pw=0.5;
    var cutoff=1000;
    var gain=2;
    var pan = 0;
	var syn;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		pg = ParGroup.tail(context.xg);
    	SynthDef("PolyPercIcarus", {
			arg out, freq = 440, pw=pw, amp=amp, cutoff=cutoff, gain=gain, release=release, pan=pan;
			var snd = Pulse.ar(freq, pw);
			var filt = MoogFF.ar(snd,cutoff,gain);
			var env = Env.perc(level: amp, releaseTime: release).kr(2);
			Out.ar(out, Pan2.ar((filt*env), pan));
		}).add;

		SynthDef("icarussynth",{ 
			arg amp=0.5, hz=220, pan=0, envgate=1, t_gate=0,
			pulse=0,saw=0,bend=0,subpitch=1,
			attack=0.015,decay=1,release=2,sustain=0.9,
			lpf=20000,resonance=0,portamento=0.1,tremelo=0,destruction=0,
			pwmcenter=0.5,pwmwidth=0.05,pwmfreq=10,detuning=0.1,
			feedback=0.5,delaytime=0.25, delaytimelag=0.1, sublevel=0.2;

			// vars
			var ender,snd,local,in,ampcheck,hz_dream,hz_sub,subdiv;
			var envpolyperc = EnvGen.ar(Env.perc(releaseTime:release),gate:t_gate);

			// envelope stuff
			ender = EnvGen.ar(
				Env.new(
					curve: 'cubed',
					levels: [0,1,sustain,0],
					times: [attack+0.015,decay,release],
					releaseNode: 2,
				),
				gate: 1,
			);

			// dreamcrusher++
			hz_dream=(Lag.kr(hz+(SinOsc.kr(LFNoise0.kr(1))*(((hz).cpsmidi+1).midicps-(hz))*detuning),portamento).cpsmidi + bend).midicps;
			in = VarSaw.ar(hz_dream,
				width:
				LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
				mul:0.5
			);
			// add suboscillator
			subdiv=2**subpitch;
			hz_sub=(Lag.kr(hz/subdiv+(SinOsc.kr(LFNoise0.kr(1))*(((hz/subdiv).cpsmidi+1).midicps-(hz/subdiv))*detuning),portamento).cpsmidi + bend).midicps;
			in = in + Pulse.ar(hz_sub,
				width:
				LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
				mul:0.5*sublevel	
			);
			in = Splay.ar(in);
			in = in * envpolyperc;



			// random panning
			in = Balance2.ar(in[0] ,in[1],SinOsc.kr(
				LinLin.kr(LFNoise0.kr(0.1),-1,1,0.05,0.2)
			)*0.1);

			in = in * ender;
			ampcheck = Amplitude.kr(Mix.ar(in));
			in = in * (ampcheck > 0.02); // noise gate
			local = LocalIn.ar(2);
			local = OnePole.ar(local, 0.4);
			local = OnePole.ar(local, -0.08);
			local = Rotate2.ar(local[0], local[1],0.2);
			local = DelayC.ar(local, 0.5,
				Lag.kr(delaytime,0.2)
			);
			local = LeakDC.ar(local);
			local = ((local + in) * 1.25).softclip;

			local = MoogLadder.ar(local,Lag.kr(lpf,1),res:Lag.kr(resonance,1));
			// add destruction thing
			local = ((local*((1-EnvGen.kr(
					Env(
						levels: [0, 1,0], 
						times: [0.1,0.1],
						curve:\sine,
					),
					gate: Dust.kr(destruction)
			))))+local)/2;
			// add tremelo
			// local = local * ((tremelo>0)*SinOsc.kr(tremelo,0,0.4)+(tremelo<0.0001));

			LocalOut.ar(local*Lag.kr(feedback,1));
			
			snd= Balance2.ar(local[0],local[1],SinOsc.kr(
				LinLin.kr(LFNoise0.kr(0.1),-1,1,0.05,0.2)
			)*0.1);

			// manual pan
			snd = Mix.ar([
				Pan2.ar(snd[0],-1+(2*pan),amp),
				Pan2.ar(snd[1],1+(2*pan),amp),
			]);
			//SendTrig.kr(Dust.kr(30.0),0,Amplitude.kr(snd[0]+snd[1],3,3));
			Out.ar(0,snd*2)
		}).add;	

		context.server.sync;

		syn = Synth("icarussynth");

		context.server.sync;

		this.addCommand("hz", "f", { arg msg;
			var val = msg[1];
			syn.set(\hz,val,\t_gate,1);
	    //   Synth("PolyPercIcarus", [\out, context.out_b, \freq,val,\pw,pw,\amp,amp,\cutoff,cutoff,\gain,gain,\release,release,\pan,pan], target:pg);
		});

		this.addCommand("amp", "f", { arg msg;
			amp = msg[1];
			syn.set(\amp,amp);
		});

		this.addCommand("pw", "f", { arg msg;
			pw = msg[1];
			syn.set(\pwmwidth,pw);
		});
		
		// engine.feedback()
		this.addCommand("feedback", "f", { arg msg;
			syn.set(\feedback,msg[1]);
		});
		
		this.addCommand("release", "f", { arg msg;
			release = msg[1];
			syn.set(\release,release);
		});
		
		this.addCommand("cutoff", "f", { arg msg;
			cutoff = msg[1];
			syn.set(\lpf,cutoff);
		});
		
		this.addCommand("gain", "f", { arg msg;
			gain = msg[1];
			syn.set(\resonance,gain);
		});
		
		this.addCommand("pan", "f", { arg msg;
		  postln("pan: " ++ msg[1]);
			pan = msg[1];
			syn.set(\pan,pan);
		});
	}

	free { 
		pg.free;
		syn.free;
	}
}
